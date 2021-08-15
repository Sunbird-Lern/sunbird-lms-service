package org.sunbird.actor.location;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;

import org.apache.commons.lang3.StringUtils;
import org.sunbird.actor.router.ActorConfig;
import org.sunbird.dto.SearchDTO;
import org.sunbird.keys.JsonKey;
import org.sunbird.service.location.LocationService;
import org.sunbird.service.location.LocationServiceImpl;
import org.sunbird.util.Util;
import org.sunbird.actor.location.validator.LocationRequestValidator;
import org.sunbird.model.location.Location;
import org.sunbird.model.location.UpsertLocationRequest;
import org.sunbird.operations.LocationActorOperation;
import org.sunbird.request.Request;
import org.sunbird.request.RequestContext;
import org.sunbird.response.Response;
import org.sunbird.telemetry.dto.TelemetryEnvKey;
import org.sunbird.util.ProjectUtil;

/**
 * This class will handle all location related request.
 *
 * @author Amit Kumar
 */
@ActorConfig(
        tasks = {
                "createLocation",
                "updateLocation",
                "searchLocation",
                "deleteLocation",
                "getRelatedLocationIds"
        },
        asyncTasks = {}
)
public class LocationActor extends BaseLocationActor {

    private LocationService locationService = LocationServiceImpl.getInstance();
    @Override
    public void onReceive(Request request) throws Throwable {
        Util.initializeContext(request, TelemetryEnvKey.LOCATION);
        String operation = request.getOperation();
        switch (operation) {
            case "createLocation":
                createLocation(request);
                break;
            case "updateLocation":
                updateLocation(request);
                break;
            case "searchLocation":
                searchLocation(request);
                break;
            case "deleteLocation":
                deleteLocation(request);
                break;
            case "getRelatedLocationIds":
                getRelatedLocationIds(request);
                break;
            default:
                onReceiveUnsupportedOperation("LocationActor");
        }
    }

    private void getRelatedLocationIds(Request request) {
        Response response = new Response();
        List<String> relatedLocationIds = locationService.
                getValidatedRelatedLocationIds(
                        (List<String>) request.get(JsonKey.LOCATION_CODES), request.getRequestContext());
        response.getResult().put(JsonKey.RESPONSE, relatedLocationIds);
        sender().tell(response, self());
    }

    private void createLocation(Request request) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            UpsertLocationRequest locationRequest =
                    ProjectUtil.convertToRequestPojo(request, UpsertLocationRequest.class);
            validateUpsertLocnReq(locationRequest, JsonKey.CREATE);
            // put unique identifier in request for Id
            String id = ProjectUtil.generateUniqueId();
            locationRequest.setId(id);
            Location location = mapper.convertValue(locationRequest, Location.class);
            Response response = locationService.createLocation(location, request.getRequestContext());
            sender().tell(response, self());
            logger.info(request.getRequestContext(), "Insert location data to ES");
            saveDataToES(
                    mapper.convertValue(location, Map.class), JsonKey.INSERT, request.getRequestContext());
            generateTelemetryForLocation(
                    id, mapper.convertValue(location, Map.class), JsonKey.CREATE, request.getContext());
        } catch (Exception ex) {
            logger.error(request.getRequestContext(), ex.getMessage(), ex);
            sender().tell(ex, self());
        }
    }

    private void updateLocation(Request request) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            UpsertLocationRequest locationRequest =
                    ProjectUtil.convertToRequestPojo(request, UpsertLocationRequest.class);
            validateUpsertLocnReq(locationRequest, JsonKey.UPDATE);
            Location location = mapper.convertValue(locationRequest, Location.class);
            Response response = locationService.updateLocation(location, request.getRequestContext());
            sender().tell(response, self());
            logger.info(request.getRequestContext(), "Update location data to ES");
            saveDataToES(
                    mapper.convertValue(location, Map.class), JsonKey.UPDATE, request.getRequestContext());
            generateTelemetryForLocation(
                    location.getId(),
                    mapper.convertValue(location, Map.class),
                    JsonKey.UPDATE,
                    request.getContext());
        } catch (Exception ex) {
            logger.error(request.getRequestContext(), ex.getMessage(), ex);
            sender().tell(ex, self());
        }
    }

    private void searchLocation(Request request) {
        try {
            Response response = locationService.searchLocation(request.getRequest(), request.getRequestContext());
            sender().tell(response, self());
            SearchDTO searchDto = Util.createSearchDto(request.getRequest());
            String[] types = {ProjectUtil.EsType.location.getTypeName()};
            generateSearchTelemetryEvent(searchDto, types, response.getResult(), request.getContext());
        } catch (Exception ex) {
            logger.error(request.getRequestContext(), ex.getMessage(), ex);
            sender().tell(ex, self());
        }
    }

    private void deleteLocation(Request request) {
        try {
            String locationId = (String) request.getRequest().get(JsonKey.LOCATION_ID);
            LocationRequestValidator.isLocationHasChild(locationId);
            Response response = locationService.deleteLocation(locationId, request.getRequestContext());
            sender().tell(response, self());
            logger.info(request.getRequestContext(), "Delete location data from ES");
            deleteDataFromES(locationId, request.getRequestContext());
            generateTelemetryForLocation(
                    locationId, new HashMap<>(), JsonKey.DELETE, request.getContext());
        } catch (Exception ex) {
            logger.error(request.getRequestContext(), ex.getMessage(), ex);
            sender().tell(ex, self());
        }
    }

    private void saveDataToES(Map<String, Object> locData, String opType, RequestContext context) {
        Request request = new Request();
        request.setRequestContext(context);
        request.setOperation(LocationActorOperation.UPSERT_LOCATION_TO_ES.getValue());
        request.getRequest().put(JsonKey.LOCATION, locData);
        request.getRequest().put(JsonKey.OPERATION_TYPE, opType);
        try {
            tellToAnother(request);
        } catch (Exception ex) {
            logger.error(context, "LocationActor:saveDataToES: Exception occurred", ex);
        }
    }

    private void deleteDataFromES(String locId, RequestContext context) {
        Request request = new Request();
        request.setRequestContext(context);
        request.setOperation(LocationActorOperation.DELETE_LOCATION_FROM_ES.getValue());
        request.getRequest().put(JsonKey.LOCATION_ID, locId);
        try {
            tellToAnother(request);
        } catch (Exception ex) {
            logger.error(context, "LocationActor:saveDataToES: Exception occurred ", ex);
        }
    }

    private void validateUpsertLocnReq(UpsertLocationRequest locationRequest, String operation) {
        if (StringUtils.isNotEmpty(locationRequest.getType())) {
            LocationRequestValidator.isValidLocationType(locationRequest.getType());
        }
        LocationRequestValidator.isValidParentIdAndCode(locationRequest, operation);
    }

}
