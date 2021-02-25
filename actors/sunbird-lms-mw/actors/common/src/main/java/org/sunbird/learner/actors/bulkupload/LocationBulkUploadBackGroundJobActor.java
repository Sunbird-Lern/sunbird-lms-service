package org.sunbird.learner.actors.bulkupload;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.sql.Timestamp;
import java.text.MessageFormat;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.actor.router.ActorConfig;
import org.sunbird.actorutil.location.LocationClient;
import org.sunbird.actorutil.location.impl.LocationClientImpl;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.util.*;
import org.sunbird.common.models.util.ProjectUtil.BulkProcessStatus;
import org.sunbird.common.request.Request;
import org.sunbird.common.request.RequestContext;
import org.sunbird.common.responsecode.ResponseCode;
import org.sunbird.learner.actors.bulkupload.model.BulkUploadProcess;
import org.sunbird.learner.actors.bulkupload.model.BulkUploadProcessTask;
import org.sunbird.learner.util.Util;
import org.sunbird.models.location.Location;
import org.sunbird.models.location.apirequest.UpsertLocationRequest;

@ActorConfig(
  tasks = {},
  asyncTasks = {"locationBulkUploadBackground"}
)
public class LocationBulkUploadBackGroundJobActor extends BaseBulkUploadBackgroundJobActor {

  private LocationClient locationClient = new LocationClientImpl();

  @Override
  public void onReceive(Request request) throws Throwable {

    String operation = request.getOperation();
    Util.initializeContext(request, TelemetryEnvKey.GEO_LOCATION);

    switch (operation) {
      case "locationBulkUploadBackground":
        handleBulkUploadBackground(
            request,
            (bulkUploadProcess) -> {
              processBulkUpload(
                  (BulkUploadProcess) bulkUploadProcess,
                  (tasks) -> {
                    processTasks((List<BulkUploadProcessTask>) tasks, request.getRequestContext());
                    return null;
                  },
                  request.getRequestContext());
              return null;
            });
        break;
      default:
        logger.info(operation + ": unsupported message");
    }
  }

  private void processLocation(BulkUploadProcessTask task, RequestContext context) {
    ObjectMapper mapper = new ObjectMapper();
    logger.info(context, "LocationBulkUploadBackGroundJobActor: processLocation called");
    String data = task.getData();
    try {

      Map<String, Object> row = mapper.readValue(data, Map.class);

      if (checkMandatoryFields(row, JsonKey.CODE)) {
        Location location = null;
        try {
          location =
              locationClient.getLocationByCode(
                  getActorRef(LocationActorOperation.SEARCH_LOCATION.getValue()),
                  (String) row.get(JsonKey.CODE),
                  context);
        } catch (Exception ex) {
          setTaskStatus(task, BulkProcessStatus.FAILED, ex.getMessage(), row, null);
        }
        if (null == location) {
          callCreateLocation(row, task, context);
        } else {
          callUpdateLocation(row, mapper.convertValue(location, Map.class), task, context);
        }
      } else {
        setTaskStatus(
            task,
            BulkProcessStatus.FAILED,
            MessageFormat.format(
                ResponseCode.mandatoryParamsMissing.getErrorMessage(), JsonKey.CODE),
            row,
            null);
      }
    } catch (IOException e) {
      ProjectCommonException.throwClientErrorException(
          ResponseCode.SERVER_ERROR, ResponseCode.SERVER_ERROR.getErrorMessage());
    }
  }

  private boolean checkMandatoryFields(Map<String, Object> row, String... fields) {

    boolean flag = true;
    for (String field : fields) {
      if (!(row.containsKey(field))) {
        flag = false;
        break;
      }
    }
    return flag;
  }

  private void callUpdateLocation(
      Map<String, Object> row,
      Map<String, Object> response,
      BulkUploadProcessTask task,
      RequestContext context)
      throws JsonProcessingException {

    String id = (String) response.get(JsonKey.ID);
    row.put(JsonKey.ID, id);

    // since for update type is not allowed so remove from request body
    String locationType = (String) row.remove(JsonKey.LOCATION_TYPE);
    // check whether update for same type or different type.
    if (!(areLocationTypesEqual(locationType, (String) response.get(JsonKey.LOCATION_TYPE)))) {
      row.put(JsonKey.LOCATION_TYPE, response.get(JsonKey.LOCATION_TYPE));
      setTaskStatus(
          task,
          BulkProcessStatus.FAILED,
          MessageFormat.format(
              ResponseCode.unupdatableField.getErrorMessage(), JsonKey.LOCATION_TYPE),
          row,
          JsonKey.UPDATE);
      return;
    }
    ObjectMapper mapper = new ObjectMapper();
    try {
      locationClient.updateLocation(
          getActorRef(LocationActorOperation.UPDATE_LOCATION.getValue()),
          mapper.convertValue(row, UpsertLocationRequest.class),
          context);
    } catch (Exception ex) {
      logger.error(
          context,
          "LocationBulkUploadBackGroundJobActor : callUpdateLocation - got exception "
              + ex.getMessage(),
          ex);
      row.put(JsonKey.ERROR_MSG, ex.getMessage());
      setTaskStatus(task, BulkProcessStatus.FAILED, ex.getMessage(), row, JsonKey.UPDATE);
    }

    row.put(JsonKey.LOCATION_TYPE, locationType);
    task.setData(mapper.writeValueAsString(row));
    setSuccessTaskStatus(task, BulkProcessStatus.COMPLETED, row, JsonKey.UPDATE);
  }

  private Boolean areLocationTypesEqual(String locationType, String responseType) {
    return (locationType.equalsIgnoreCase(responseType));
  }

  private void callCreateLocation(
      Map<String, Object> row, BulkUploadProcessTask task, RequestContext context)
      throws JsonProcessingException {

    Request request = new Request();
    request.getRequest().putAll(row);
    request.setRequestContext(context);
    ObjectMapper mapper = new ObjectMapper();
    String locationId = "";
    try {
      locationId =
          locationClient.createLocation(
              getActorRef(LocationActorOperation.CREATE_LOCATION.getValue()),
              mapper.convertValue(row, UpsertLocationRequest.class),
              context);
    } catch (Exception ex) {
      logger.error(
          context,
          "LocationBulkUploadBackGroundJobActor : callCreateLocation - got exception "
              + ex.getMessage(),
          ex);
      setTaskStatus(task, BulkProcessStatus.FAILED, ex.getMessage(), row, JsonKey.CREATE);
      return;
    }

    if (StringUtils.isEmpty(locationId)) {
      logger.info(
          context,
          "LocationBulkUploadBackGroundJobActor : Null receive from interservice communication");
      setTaskStatus(
          task,
          BulkProcessStatus.FAILED,
          ResponseCode.internalError.getErrorMessage(),
          row,
          JsonKey.CREATE);
    } else {
      row.put(JsonKey.ID, locationId);
      setSuccessTaskStatus(task, BulkProcessStatus.COMPLETED, row, JsonKey.CREATE);
    }
  }

  private void processTasks(List<BulkUploadProcessTask> tasks, RequestContext context) {
    for (BulkUploadProcessTask task : tasks) {
      if (task.getStatus() != null
          && task.getStatus() != ProjectUtil.BulkProcessStatus.COMPLETED.getValue()) {
        processLocation(task, context);
        task.setLastUpdatedOn(new Timestamp(System.currentTimeMillis()));
        task.setIterationId(task.getIterationId() + 1);
      }
    }
  }

  @Override
  public void preProcessResult(Map<String, Object> result) {
    // Do nothing
  }
}
