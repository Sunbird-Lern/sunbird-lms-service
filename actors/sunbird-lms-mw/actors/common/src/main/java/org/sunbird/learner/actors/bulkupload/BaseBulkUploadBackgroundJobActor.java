package org.sunbird.learner.actors.bulkupload;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.*;
import java.util.function.Function;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.common.Constants;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.util.BulkUploadJsonKey;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.request.Request;
import org.sunbird.common.request.RequestContext;
import org.sunbird.common.responsecode.ResponseCode;
import org.sunbird.learner.actors.bulkupload.dao.BulkUploadProcessDao;
import org.sunbird.learner.actors.bulkupload.dao.BulkUploadProcessTaskDao;
import org.sunbird.learner.actors.bulkupload.dao.impl.BulkUploadProcessDaoImpl;
import org.sunbird.learner.actors.bulkupload.dao.impl.BulkUploadProcessTaskDaoImpl;
import org.sunbird.learner.actors.bulkupload.model.BulkUploadProcess;
import org.sunbird.learner.actors.bulkupload.model.BulkUploadProcessTask;

public abstract class BaseBulkUploadBackgroundJobActor extends BaseBulkUploadActor {

  protected void setSuccessTaskStatus(
      BulkUploadProcessTask task,
      ProjectUtil.BulkProcessStatus status,
      Map<String, Object> row,
      String action)
      throws JsonProcessingException {
    ObjectMapper mapper = new ObjectMapper();
    row.put(JsonKey.OPERATION, action);
    task.setSuccessResult(mapper.writeValueAsString(row));
    task.setStatus(status.getValue());
  }

  protected void setTaskStatus(
      BulkUploadProcessTask task,
      ProjectUtil.BulkProcessStatus status,
      String failureMessage,
      Map<String, Object> row,
      String action)
      throws JsonProcessingException {
    ObjectMapper mapper = new ObjectMapper();
    row.put(JsonKey.OPERATION, action);
    if (ProjectUtil.BulkProcessStatus.COMPLETED.getValue() == status.getValue()) {
      task.setSuccessResult(mapper.writeValueAsString(row));
      task.setStatus(status.getValue());
    } else if (ProjectUtil.BulkProcessStatus.FAILED.getValue() == status.getValue()) {
      row.put(JsonKey.ERROR_MSG, failureMessage);
      task.setStatus(status.getValue());
      task.setFailureResult(mapper.writeValueAsString(row));
    }
  }

  public void handleBulkUploadBackground(Request request, Function function) {
    String processId = (String) request.get(JsonKey.PROCESS_ID);
    BulkUploadProcessDao bulkUploadDao = new BulkUploadProcessDaoImpl();
    String logMessagePrefix =
        MessageFormat.format(
            "BaseBulkUploadBackGroundJobActor:handleBulkUploadBackground:{0}: ", processId);

    logger.info(request.getRequestContext(), logMessagePrefix + "called");

    BulkUploadProcess bulkUploadProcess =
        bulkUploadDao.read(processId, request.getRequestContext());
    if (null == bulkUploadProcess) {
      logger.info(request.getRequestContext(), logMessagePrefix + "Invalid process ID.");
      return;
    }

    int status = bulkUploadProcess.getStatus();
    if (!(ProjectUtil.BulkProcessStatus.COMPLETED.getValue() == status)
        || ProjectUtil.BulkProcessStatus.INTERRUPT.getValue() == status) {
      try {
        function.apply(bulkUploadProcess);
      } catch (Exception e) {
        bulkUploadProcess.setStatus(ProjectUtil.BulkProcessStatus.FAILED.getValue());
        bulkUploadProcess.setFailureResult(e.getMessage());
        bulkUploadDao.update(bulkUploadProcess, null);
        logger.error(
            request.getRequestContext(),
            logMessagePrefix + "Exception occurred with error message = " + e.getMessage(),
            e);
      }
    }

    bulkUploadProcess.setStatus(ProjectUtil.BulkProcessStatus.COMPLETED.getValue());
    bulkUploadDao.update(bulkUploadProcess, request.getRequestContext());
  }

  public void processBulkUpload(
      BulkUploadProcess bulkUploadProcess, Function function, RequestContext context) {
    BulkUploadProcessTaskDao bulkUploadProcessTaskDao = new BulkUploadProcessTaskDaoImpl();
    String logMessagePrefix =
        MessageFormat.format(
            "BaseBulkUploadBackGroundJobActor:processBulkUpload:{0}: ", bulkUploadProcess.getId());
    Integer sequence = 0;
    Integer taskCount = bulkUploadProcess.getTaskCount();
    List<Map<String, Object>> successList = new LinkedList<>();
    List<Map<String, Object>> failureList = new LinkedList<>();
    while (sequence < taskCount) {
      Integer nextSequence = sequence + getBatchSize(JsonKey.CASSANDRA_WRITE_BATCH_SIZE);
      Map<String, Object> queryMap = new HashMap<>();
      queryMap.put(JsonKey.PROCESS_ID, bulkUploadProcess.getId());
      Map<String, Object> sequenceRange = new HashMap<>();
      sequenceRange.put(Constants.GT, sequence);
      sequenceRange.put(Constants.LTE, nextSequence);
      queryMap.put(BulkUploadJsonKey.SEQUENCE_ID, sequenceRange);
      List<BulkUploadProcessTask> tasks =
          bulkUploadProcessTaskDao.readByPrimaryKeys(queryMap, context);
      if (tasks == null) {
        logger.info(
            context,
            logMessagePrefix
                + "No bulkUploadProcessTask found for process id: "
                + bulkUploadProcess.getId()
                + " and range "
                + sequence
                + ":"
                + nextSequence);
        sequence = nextSequence;
        continue;
      }
      function.apply(tasks);

      try {
        ObjectMapper mapper = new ObjectMapper();
        for (BulkUploadProcessTask task : tasks) {

          if (task.getStatus().equals(ProjectUtil.BulkProcessStatus.FAILED.getValue())) {
            failureList.add(
                mapper.readValue(
                    task.getFailureResult(), new TypeReference<Map<String, Object>>() {}));
          } else if (task.getStatus().equals(ProjectUtil.BulkProcessStatus.COMPLETED.getValue())) {
            successList.add(
                mapper.readValue(
                    task.getSuccessResult(), new TypeReference<Map<String, Object>>() {}));
          }
        }

      } catch (IOException e) {
        logger.error(
            context,
            logMessagePrefix + "Exception occurred with error message = " + e.getMessage(),
            e);
      }
      performBatchUpdate(tasks, context);
      sequence = nextSequence;
    }
    setCompletionStatus(bulkUploadProcess, successList, failureList, context);
  }

  private void setCompletionStatus(
      BulkUploadProcess bulkUploadProcess,
      List successList,
      List failureList,
      RequestContext context) {
    String logMessagePrefix =
        MessageFormat.format(
            "BaseBulkUploadBackGroundJobActor:processBulkUpload:{0}: ", bulkUploadProcess.getId());
    bulkUploadProcess.setSuccessResult(ProjectUtil.convertMapToJsonString(successList));
    bulkUploadProcess.setFailureResult(ProjectUtil.convertMapToJsonString(failureList));
    bulkUploadProcess.setStatus(ProjectUtil.BulkProcessStatus.COMPLETED.getValue());
    logger.info(context, logMessagePrefix + "completed");
    BulkUploadProcessDao bulkUploadDao = new BulkUploadProcessDaoImpl();
    bulkUploadDao.update(bulkUploadProcess, context);
  }

  protected void validateMandatoryFields(
      Map<String, Object> csvColumns, BulkUploadProcessTask task, String[] mandatoryFields)
      throws JsonProcessingException {
    if (mandatoryFields != null) {
      for (String field : mandatoryFields) {
        if (StringUtils.isEmpty((String) csvColumns.get(field))) {
          String errorMessage =
              MessageFormat.format(
                  ResponseCode.mandatoryParamsMissing.getErrorMessage(), new Object[] {field});

          setTaskStatus(
              task, ProjectUtil.BulkProcessStatus.FAILED, errorMessage, csvColumns, JsonKey.CREATE);

          ProjectCommonException.throwClientErrorException(
              ResponseCode.mandatoryParamsMissing, errorMessage);
        }
      }
    }
  }

  public abstract void preProcessResult(Map<String, Object> result);
}
