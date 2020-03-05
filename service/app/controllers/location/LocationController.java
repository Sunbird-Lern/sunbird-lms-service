package controllers.location;

import com.fasterxml.jackson.databind.JsonNode;
import controllers.BaseController;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.LocationActorOperation;
import org.sunbird.common.request.Request;
import org.sunbird.validator.location.BaseLocationRequestValidator;
import play.mvc.Http;
import play.mvc.Result;

/**
 * LocationController handles location APIs.
 *
 * @author arvind on 18/4/18.
 */
public class LocationController extends BaseController {

  BaseLocationRequestValidator validator = new BaseLocationRequestValidator();
  /**
   * Method to create new location.
   *
   * <p>Request body contains following parameters - name: A name given to location . code: Unique
   * code for the location. type: Each location has specific type for example location type can be
   * STATE, DISTRICT, BLOCK, CLUSTER. parentId: The location has hierarchy , so the parentId defines
   * the one level up parentId of location ,for root level location parentId not required and other
   * than root location parentId required.
   *
   * @return Return a promise for create location API result
   */
  public CompletionStage<Result> createLocation(Http.Request httpRequest) {

    try {
      JsonNode jsonNode = httpRequest.body().asJson();
      Request request =
          createAndInitRequest(LocationActorOperation.CREATE_LOCATION.getValue(), jsonNode, httpRequest);
      validator.validateCreateLocationRequest(request);
      return actorResponseHandler(getActorRef(), request, timeout, null, httpRequest);
    } catch (Exception e) {
      return CompletableFuture.completedFuture(createCommonExceptionResponse(e, httpRequest));
    }
  }

  /**
   * Method to update the location.
   *
   * <p>Request body contains following parameters - id: Id of the location to uniquely identify the
   * location , Every location assigned with unique identifier while location creation.The fields
   * that can not be updated are code, type. The allowed fields that can be updated are -
   * parentCode, parentId, name. The parentId or parentCode should be of the immediate one level up
   * location id. For root level location can not update parentId or parentCode.
   *
   * @return Return a promise for update location API result
   */
  public CompletionStage<Result> updateLocation(Http.Request httpRequest) {

    try {
      JsonNode jsonNode = httpRequest.body().asJson();
      Request request =
          createAndInitRequest(LocationActorOperation.UPDATE_LOCATION.getValue(), jsonNode, httpRequest);
      validator.validateUpdateLocationRequest(request);
      return actorResponseHandler(getActorRef(), request, timeout, null, httpRequest);
    } catch (Exception e) {
      return CompletableFuture.completedFuture(createCommonExceptionResponse(e, httpRequest));
    }
  }

  /**
   * Method to delete the location on basis of location id .
   *
   * <p>Path param contains locationId .
   *
   * @param locationId Id of location to
   * @return Return a promise for update location API result.
   */
  public CompletionStage<Result> deleteLocation(String locationId, Http.Request httpRequest) {
    try {
      Request request = createAndInitRequest(LocationActorOperation.DELETE_LOCATION.getValue(), httpRequest);
      validator.validateDeleteLocationRequest(locationId);
      Map<String, Object> requestMap = request.getRequest();
      requestMap.put(JsonKey.LOCATION_ID, locationId);
      return actorResponseHandler(getActorRef(), request, timeout, null, httpRequest);
    } catch (Exception e) {
      return CompletableFuture.completedFuture(createCommonExceptionResponse(e, httpRequest));
    }
  }

  /**
   * Method to search the location on basis of search query in request body.
   *
   * <p>Request body contains the various filters on which basis location search execute and also
   * parameters like offset and size. For example parameter filter contain the fields and their
   * values , parameter size represents the max search result size.
   *
   * @return Return a promise for update location API result.
   */
  public CompletionStage<Result> searchLocation(Http.Request httpRequest) {
    try {
      JsonNode jsonNode = httpRequest.body().asJson();
      Request request =
          createAndInitRequest(LocationActorOperation.SEARCH_LOCATION.getValue(), jsonNode, httpRequest);
      validator.validateSearchLocationRequest(request);
      return actorResponseHandler(getActorRef(), request, timeout, null, httpRequest);
    } catch (Exception e) {
      return CompletableFuture.completedFuture(createCommonExceptionResponse(e, httpRequest));
    }
  }
}
