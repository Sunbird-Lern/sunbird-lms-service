/** */
package controllers.usermanagement;

import static org.sunbird.learner.util.Util.isNotNull;
import static org.sunbird.learner.util.Util.isNull;

import com.fasterxml.jackson.databind.JsonNode;
import controllers.BaseController;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.LoggerEnum;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.models.util.ProjectUtil.EsType;
import org.sunbird.common.models.util.StringFormatter;
import org.sunbird.common.request.ExecutionContext;
import org.sunbird.common.request.HeaderParam;
import org.sunbird.common.request.Request;
import org.sunbird.common.request.UserRequestValidator;
import org.sunbird.common.responsecode.ResponseCode;
import play.libs.F.Promise;
import play.mvc.Result;
import util.AuthenticationHelper;

/**
 * This controller will handle all the request and responses for user management.
 *
 * @author Manzarul
 */
public class UserController extends BaseController {

  /**
   * This method will do the registration process. registered user data will be store inside
   * cassandra db.
   *
   * @return Promise<Result>
   */
  public Promise<Result> createUser() {
    try {
      ProjectLogger.log("UserController: createUserV1 called", LoggerEnum.INFO.name());
      JsonNode requestData = request().body().asJson();
      Request request =
          createAndInitRequest(ActorOperations.CREATE_USER.getValue(), requestData);
      UserRequestValidator.validateCreateUser(request);
      UserRequestValidator.fieldsNotAllowed(Arrays.asList(JsonKey.ORGANISATION_ID), request);
      HashMap<String, Object> innerMap = new HashMap<>();
      ProjectUtil.updateMapSomeValueTOLowerCase(request);
      innerMap.put(JsonKey.USER, request.getRequest());
      innerMap.put(JsonKey.REQUESTED_BY, ctx().flash().get(JsonKey.USER_ID));
      request.setRequest(innerMap);
      return actorResponseHandler(getActorRef(), request, timeout, null, request());
    } catch (Exception e) {
      return Promise.<Result>pure(createCommonExceptionResponse(e, request()));
    }
  }
  
  /**
   * This method will do the registration process. registered user data will be store inside
   * cassandra db.
   *
   * @return Promise<Result>
   */
  public Promise<Result> createUserV2() {
    try {
      ProjectLogger.log("UserController: createUserV2 called", LoggerEnum.INFO.name());
      JsonNode requestData = request().body().asJson();
      Request request =
          createAndInitRequest(ActorOperations.CREATE_USER.getValue(), requestData);
      UserRequestValidator.validateCreateUserV2(request);
      HashMap<String, Object> innerMap = new HashMap<>();
      ProjectUtil.updateMapSomeValueTOLowerCase(request);
      innerMap.put(JsonKey.USER, request.getRequest());
      innerMap.put(JsonKey.REQUESTED_BY, ctx().flash().get(JsonKey.USER_ID));
      request.setRequest(innerMap);
      request.getRequest().put(JsonKey.VERSION,JsonKey.VERSION_2);
      return actorResponseHandler(getActorRef(), request, timeout, null, request());
    } catch (Exception e) {
      return Promise.<Result>pure(createCommonExceptionResponse(e, request()));
    }
  }

  /**
   * This method will update user profile data. user can update all the data except email.
   *
   * @return Promise<Result>
   */
  public Promise<Result> updateUserProfile() {

    try {
      JsonNode requestData = request().body().asJson();
      ProjectLogger.log("UserController: updateUserProfile called", LoggerEnum.DEBUG.name());
      Request reqObj = (Request) mapper.RequestMapper.mapRequest(requestData, Request.class);
      String accessToken = request().getHeader(HeaderParam.X_Authenticated_User_Token.getName());
      reqObj.getRequest().put(HeaderParam.X_Authenticated_User_Token.getName(), accessToken);
      UserRequestValidator.validateUpdateUser(reqObj);
      if (null != ctx().flash().get(JsonKey.IS_AUTH_REQ)
          && Boolean.parseBoolean(ctx().flash().get(JsonKey.IS_AUTH_REQ))) {
        validateAuthenticity(reqObj);
      }
      ProjectUtil.updateMapSomeValueTOLowerCase(reqObj);
      reqObj.setOperation(ActorOperations.UPDATE_USER.getValue());
      reqObj.setRequestId(ExecutionContext.getRequestId());
      reqObj.setEnv(getEnvironment());
      HashMap<String, Object> innerMap = new HashMap<>();
      innerMap.put(JsonKey.USER, reqObj.getRequest());

      innerMap.put(JsonKey.REQUESTED_BY, ctx().flash().get(JsonKey.USER_ID));
      reqObj.setRequest(innerMap);
      return actorResponseHandler(getActorRef(), reqObj, timeout, null, request());
    } catch (Exception e) {
      return Promise.<Result>pure(createCommonExceptionResponse(e, request()));
    }
  }

  private void validateAuthenticity(Request reqObj) {

    if (ctx().flash().containsKey(JsonKey.AUTH_WITH_MASTER_KEY)) {
      validateWithClient(reqObj);
    } else {
      ProjectLogger.log("Auth token is not master token.");
      validateWithUserId(reqObj);
    }
  }

  private String getUserIdFromExtIdAndProvider(Request reqObj) {
    String userId = "";
    if (null != reqObj.getRequest().get(JsonKey.USER_ID)) {
      userId = (String) reqObj.getRequest().get(JsonKey.USER_ID);
    } else {
      userId = (String) reqObj.getRequest().get(JsonKey.ID);
    }
    if (StringUtils.isBlank(userId)) {
      String extId = (String) reqObj.getRequest().get(JsonKey.EXTERNAL_ID);
      String provider = (String) reqObj.getRequest().get(JsonKey.EXTERNAL_ID_PROVIDER);
      String idType = (String) reqObj.getRequest().get(JsonKey.EXTERNAL_ID_TYPE);
      Map<String, Object> user =
          AuthenticationHelper.getUserFromExternalId(extId, provider, idType);
      if (MapUtils.isNotEmpty(user)) {
        userId = (String) user.get(JsonKey.ID);
      } else {
        throw new ProjectCommonException(
            ResponseCode.invalidParameter.getErrorCode(),
            ProjectUtil.formatMessage(
                ResponseCode.invalidParameter.getErrorMessage(),
                StringFormatter.joinByAnd(
                    StringFormatter.joinByComma(JsonKey.EXTERNAL_ID, JsonKey.EXTERNAL_ID_TYPE),
                    JsonKey.EXTERNAL_ID_PROVIDER)),
            ResponseCode.CLIENT_ERROR.getResponseCode());
      }
    }
    return userId;
  }

  private void validateWithClient(Request reqObj) {
    String clientId = ctx().flash().get(JsonKey.USER_ID);
    String userId = getUserIdFromExtIdAndProvider(reqObj);

    Map<String, Object> clientDetail = AuthenticationHelper.getClientAccessTokenDetail(clientId);
    // get user detail from cassandra
    Map<String, Object> userDetail = AuthenticationHelper.getUserDetail(userId);
    // check whether both exist or not ...
    if (clientDetail == null || userDetail == null) {
      throw new ProjectCommonException(
          ResponseCode.unAuthorized.getErrorCode(),
          ResponseCode.unAuthorized.getErrorMessage(),
          ResponseCode.UNAUTHORIZED.getResponseCode());
    }

    String userRootOrgId = (String) userDetail.get(JsonKey.ROOT_ORG_ID);
    if (StringUtils.isBlank(userRootOrgId)) {
      throw new ProjectCommonException(
          ResponseCode.unAuthorized.getErrorCode(),
          ResponseCode.unAuthorized.getErrorMessage(),
          ResponseCode.UNAUTHORIZED.getResponseCode());
    }
    // get the org info from org table
    Map<String, Object> orgDetail = AuthenticationHelper.getOrgDetail(userRootOrgId);
    String userChannel = (String) orgDetail.get(JsonKey.CHANNEL);
    String clientChannel = (String) clientDetail.get(JsonKey.CHANNEL);
    ProjectLogger.log("User channel : " + userChannel);
    ProjectLogger.log("Client channel : " + clientChannel);

    // check whether both belongs to the same channel or not ...
    if (!compareStrings(userChannel, clientChannel)) {
      throw new ProjectCommonException(
          ResponseCode.unAuthorized.getErrorCode(),
          ResponseCode.unAuthorized.getErrorMessage(),
          ResponseCode.UNAUTHORIZED.getResponseCode());
    }
  }

  private void validateWithUserId(Request reqObj) {
    String userId = getUserIdFromExtIdAndProvider(reqObj);
    if ((!StringUtils.isBlank(userId)) && (!userId.equals(ctx().flash().get(JsonKey.USER_ID)))) {
      throw new ProjectCommonException(
          ResponseCode.unAuthorized.getErrorCode(),
          ResponseCode.unAuthorized.getErrorMessage(),
          ResponseCode.UNAUTHORIZED.getResponseCode());
    }
  }

  /**
   * This method will provide user profile details based on requested userId.
   *
   * @return Promise<Result>
   */
  public Promise<Result> getUserProfile(String userId) {

    try {
      String requestedFields = request().getQueryString(JsonKey.FIELDS);
      ProjectLogger.log(
          "UserController: getUserProfile called with data = " + userId, LoggerEnum.DEBUG.name());
      Request reqObj = new Request();
      reqObj.setOperation(ActorOperations.GET_PROFILE.getValue());
      reqObj.setRequestId(ExecutionContext.getRequestId());
      reqObj.setEnv(getEnvironment());
      HashMap<String, Object> innerMap = new HashMap<>();
      reqObj.getRequest().put(JsonKey.USER_ID, userId);
      innerMap.put(JsonKey.USER, reqObj.getRequest());
      innerMap.put(JsonKey.FIELDS, requestedFields);
      innerMap.put(JsonKey.REQUESTED_BY, ctx().flash().get(JsonKey.USER_ID));
      reqObj.setRequest(innerMap);
      return actorResponseHandler(getActorRef(), reqObj, timeout, null, request());
    } catch (Exception e) {
      return Promise.<Result>pure(createCommonExceptionResponse(e, request()));
    }
  }

  /**
   * This method will provide complete role details list.
   *
   * @return Promise<Result>
   */
  public Promise<Result> getRoles() {

    try {
      Request reqObj = new Request();
      reqObj.setOperation(ActorOperations.GET_ROLES.getValue());
      reqObj.setRequestId(ExecutionContext.getRequestId());
      reqObj.setEnv(getEnvironment());
      HashMap<String, Object> innerMap = new HashMap<>();
      reqObj.setRequest(innerMap);
      return actorResponseHandler(getActorRef(), reqObj, timeout, null, request());
    } catch (Exception e) {
      return Promise.<Result>pure(createCommonExceptionResponse(e, request()));
    }
  }

  /**
   * Method to verify user existence in our DB.
   *
   * @return Promise<Result>
   */
  public Promise<Result> getUserDetailsByLoginId() {

    try {
      JsonNode requestData = request().body().asJson();
      ProjectLogger.log(
          "UserController: getUserDetailsByLoginId called with data = " + requestData,
          LoggerEnum.DEBUG.name());
      Request reqObj = (Request) mapper.RequestMapper.mapRequest(requestData, Request.class);
      UserRequestValidator.validateVerifyUser(reqObj);
      reqObj.setOperation(ActorOperations.GET_USER_DETAILS_BY_LOGINID.getValue());
      reqObj.setRequestId(ExecutionContext.getRequestId());
      reqObj.setEnv(getEnvironment());
      HashMap<String, Object> innerMap = new HashMap<>();
      ProjectUtil.updateMapSomeValueTOLowerCase(reqObj);
      innerMap.put(JsonKey.USER, reqObj.getRequest());
      innerMap.put(JsonKey.FIELDS, reqObj.getRequest().get(JsonKey.FIELDS));
      innerMap.put(JsonKey.REQUESTED_BY, ctx().flash().get(JsonKey.USER_ID));
      reqObj.setRequest(innerMap);
      return actorResponseHandler(getActorRef(), reqObj, timeout, null, request());
    } catch (Exception e) {
      return Promise.<Result>pure(createCommonExceptionResponse(e, request()));
    }
  }

  /**
   * This method will provide user profile details based on requested userId.
   *
   * @return Promise<Result>
   */
  public Promise<Result> blockUser() {

    try {
      JsonNode requestData = request().body().asJson();
      ProjectLogger.log(
          "UserController: blockUser called with data = " + requestData, LoggerEnum.DEBUG.name());
      Request reqObj = (Request) mapper.RequestMapper.mapRequest(requestData, Request.class);
      reqObj.setOperation(ActorOperations.BLOCK_USER.getValue());
      reqObj.setRequestId(ExecutionContext.getRequestId());
      reqObj.setEnv(getEnvironment());
      HashMap<String, Object> innerMap = new HashMap<>();
      innerMap.put(JsonKey.USER, reqObj.getRequest());
      innerMap.put(JsonKey.REQUESTED_BY, ctx().flash().get(JsonKey.USER_ID));
      reqObj.setRequest(innerMap);
      return actorResponseHandler(getActorRef(), reqObj, timeout, null, request());
    } catch (Exception e) {
      return Promise.<Result>pure(createCommonExceptionResponse(e, request()));
    }
  }

  /**
   * This method will assign either user role directly or user org role.
   *
   * @return Promise<Result>
   */
  public Promise<Result> assignRoles() {
    try {
      JsonNode requestData = request().body().asJson();
      ProjectLogger.log(
          "UserController: assignRoles called with data = " + requestData, LoggerEnum.DEBUG.name());
      Request reqObj = (Request) mapper.RequestMapper.mapRequest(requestData, Request.class);
      UserRequestValidator.validateAssignRole(reqObj);
      reqObj.setOperation(ActorOperations.ASSIGN_ROLES.getValue());
      reqObj.setRequestId(ExecutionContext.getRequestId());
      reqObj.setEnv(getEnvironment());
      reqObj.getRequest().put(JsonKey.REQUESTED_BY, ctx().flash().get(JsonKey.USER_ID));
      return actorResponseHandler(getActorRef(), reqObj, timeout, null, request());
    } catch (Exception e) {
      return Promise.<Result>pure(createCommonExceptionResponse(e, request()));
    }
  }

  /**
   * This method will changes user status from block to unblock
   *
   * @return Promise<Result>
   */
  public Promise<Result> unBlockUser() {

    try {
      JsonNode requestData = request().body().asJson();
      ProjectLogger.log(
          "UserController: unBlockUser called with data = " + requestData, LoggerEnum.DEBUG.name());
      Request reqObj = (Request) mapper.RequestMapper.mapRequest(requestData, Request.class);
      reqObj.setOperation(ActorOperations.UNBLOCK_USER.getValue());
      reqObj.setRequestId(ExecutionContext.getRequestId());
      reqObj.setEnv(getEnvironment());
      HashMap<String, Object> innerMap = new HashMap<>();
      innerMap.put(JsonKey.USER, reqObj.getRequest());
      innerMap.put(JsonKey.REQUESTED_BY, ctx().flash().get(JsonKey.USER_ID));
      reqObj.setRequest(innerMap);
      return actorResponseHandler(getActorRef(), reqObj, timeout, null, request());
    } catch (Exception e) {
      return Promise.<Result>pure(createCommonExceptionResponse(e, request()));
    }
  }

  /**
   * This method will do the user search for Elastic search. this will internally call composite
   * search api.
   *
   * @return Promise<Result>
   */
  @SuppressWarnings({"unchecked", "rawtypes"})
  public Promise<Result> search() {
    try {
      JsonNode requestData = request().body().asJson();
      ProjectLogger.log("UserController: search call start");
      Request reqObj = (Request) mapper.RequestMapper.mapRequest(requestData, Request.class);
      reqObj.setOperation(ActorOperations.COMPOSITE_SEARCH.getValue());
      reqObj.setRequestId(ExecutionContext.getRequestId());
      reqObj.setEnv(getEnvironment());
      reqObj.put(JsonKey.REQUESTED_BY, ctx().flash().get(JsonKey.USER_ID));

      List<String> esObjectType = new ArrayList<>();
      esObjectType.add(EsType.user.getTypeName());
      if (reqObj.getRequest().containsKey(JsonKey.FILTERS)
          && reqObj.getRequest().get(JsonKey.FILTERS) != null
          && reqObj.getRequest().get(JsonKey.FILTERS) instanceof Map) {
        ((Map) (reqObj.getRequest().get(JsonKey.FILTERS))).put(JsonKey.OBJECT_TYPE, esObjectType);
      } else {
        Map<String, Object> filtermap = new HashMap<>();
        Map<String, Object> dataMap = new HashMap<>();
        dataMap.put(JsonKey.OBJECT_TYPE, esObjectType);
        filtermap.put(JsonKey.FILTERS, dataMap);
      }
      return actorResponseHandler(getActorRef(), reqObj, timeout, null, request());
    } catch (Exception e) {
      return Promise.<Result>pure(createCommonExceptionResponse(e, request()));
    }
  }

  /**
   * This method will update user current login time to keyCloack.
   *
   * @return promise<Result>
   */
  public Promise<Result> updateLoginTime() {
    ProjectLogger.log("UserController: updateLoginTime called", LoggerEnum.DEBUG.name());
    try {
      String userId = ctx().flash().get(JsonKey.USER_ID);
      JsonNode requestData = request().body().asJson();
      Request reqObj = (Request) mapper.RequestMapper.mapRequest(requestData, Request.class);
      if (reqObj == null) {
        reqObj = new Request();
      }
      reqObj.setOperation(ActorOperations.USER_CURRENT_LOGIN.getValue());
      reqObj.setRequestId(ExecutionContext.getRequestId());
      reqObj.setEnv(getEnvironment());
      if (!StringUtils.isBlank(userId)) {
        reqObj.getRequest().put(JsonKey.USER_ID, userId);
      }
      return actorResponseHandler(getActorRef(), reqObj, timeout, null, request());
    } catch (Exception e) {
      return Promise.<Result>pure(createCommonExceptionResponse(e, request()));
    }
  }

  /**
   * Get all the social media types supported
   *
   * @return
   */
  public Promise<Result> getMediaTypes() {
    try {
      ProjectLogger.log("UserController: getMediaTypes called", LoggerEnum.DEBUG.name());
      Request reqObj = new Request();
      reqObj.setOperation(ActorOperations.GET_MEDIA_TYPES.getValue());
      reqObj.setRequestId(ExecutionContext.getRequestId());
      reqObj.setEnv(getEnvironment());
      HashMap<String, Object> innerMap = new HashMap<>();
      innerMap.put(
          JsonKey.REQUESTED_BY,
          getUserIdByAuthToken(request().getHeader(HeaderParam.X_Authenticated_Userid.getName())));
      reqObj.setRequest(innerMap);
      return actorResponseHandler(getActorRef(), reqObj, timeout, null, request());
    } catch (Exception e) {
      return Promise.<Result>pure(createCommonExceptionResponse(e, request()));
    }
  }

  /**
   * This method will add or update user profile visibility control. User can make all field as
   * private except name. any private filed of user is not search-able.
   *
   * @return Promise<Result>
   */
  public Promise<Result> profileVisibility() {
    try {
      JsonNode requestData = request().body().asJson();
      ProjectLogger.log("UserController: profileVisibility called", LoggerEnum.DEBUG.name());
      Request reqObj = (Request) mapper.RequestMapper.mapRequest(requestData, Request.class);
      UserRequestValidator.validateProfileVisibility(reqObj);
      if (null != ctx().flash().get(JsonKey.IS_AUTH_REQ)
          && Boolean.parseBoolean(ctx().flash().get(JsonKey.IS_AUTH_REQ))) {
        String userId = (String) reqObj.getRequest().get(JsonKey.USER_ID);
        if (!userId.equals(ctx().flash().get(JsonKey.USER_ID))) {
          throw new ProjectCommonException(
              ResponseCode.unAuthorized.getErrorCode(),
              ResponseCode.unAuthorized.getErrorMessage(),
              ResponseCode.UNAUTHORIZED.getResponseCode());
        }
      }
      reqObj.setOperation(ActorOperations.PROFILE_VISIBILITY.getValue());
      reqObj.setRequestId(ExecutionContext.getRequestId());
      reqObj.setEnv(getEnvironment());
      HashMap<String, Object> innerMap = new HashMap<>();
      innerMap.put(JsonKey.USER, reqObj.getRequest());
      innerMap.put(JsonKey.REQUESTED_BY, ctx().flash().get(JsonKey.USER_ID));
      reqObj.setRequest(innerMap);
      return actorResponseHandler(getActorRef(), reqObj, timeout, null, request());
    } catch (Exception e) {
      return Promise.<Result>pure(createCommonExceptionResponse(e, request()));
    }
  }

  // method will compare two strings and return true id both are same otherwise false ...
  private boolean compareStrings(String first, String second) {
    if (isNull(first) && isNull(second)) {
      return true;
    }
    if ((isNull(first) && isNotNull(second)) || (isNull(second) && isNotNull(first))) {
      return false;
    }
    return first.equalsIgnoreCase(second);
  }
}
