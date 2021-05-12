package org.sunbird.user.actors;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.actor.core.BaseActor;
import org.sunbird.actor.router.ActorConfig;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.request.Request;
import org.sunbird.common.request.RequestContext;
import org.sunbird.common.responsecode.ResponseCode;
import org.sunbird.learner.util.Util;
import org.sunbird.user.dao.UserDao;
import org.sunbird.user.dao.impl.UserDaoImpl;
import org.sunbird.user.service.UserTncService;

@ActorConfig(
  tasks = {"userTnCAccept"},
  asyncTasks = {},
  dispatcher = "most-used-two-dispatcher"
)
public class UserTnCActor extends BaseActor {
  private UserTncService tncService = new UserTncService();
  private ObjectMapper mapper = new ObjectMapper();

  UserDao userDao = UserDaoImpl.getInstance();

  @Override
  public void onReceive(Request request) throws Throwable {
    String operation = request.getOperation();
    if (operation.equalsIgnoreCase(ActorOperations.USER_TNC_ACCEPT.getValue())) {
      acceptTNC(request);
    } else {
      onReceiveUnsupportedOperation("UserTnCActor");
    }
  }

  private void acceptTNC(Request request) {
    Util.initializeContext(request, JsonKey.USER);
    RequestContext requestContext = request.getRequestContext();
    Map<String, Object> context = request.getContext();
    String acceptedTnC = (String) request.getRequest().get(JsonKey.VERSION);
    String userId = (String) request.getContext().get(JsonKey.REQUESTED_BY);
    // if managedUserId's terms and conditions are accepted, get userId from request
    String managedUserId = (String) request.getRequest().get(JsonKey.USER_ID);

    boolean isManagedUser = false;
    if (StringUtils.isNotBlank(managedUserId) && !managedUserId.equals(userId)) {
      userId = managedUserId;
      isManagedUser = true;
    }

    String tncType = tncService.getTncType(request);
    tncService.validateLatestTncVersion(request, tncType);
    Map<String, Object> user = tncService.getUserById(userId, requestContext);
    tncService.isAccountManagedUser(isManagedUser, user);
    tncService.validateRoleForTnc(requestContext, tncType, user);
    if (JsonKey.TNC_CONFIG.equals(tncType)) {
      updateUserTncConfig(user, acceptedTnC, requestContext, context);
    } else {
      // update user tnc other that tncConfig
      updateUserTnc(user, acceptedTnC, tncType, requestContext, context);
    }
  }

  private void updateUserTnc(
      Map<String, Object> user,
      String acceptedTnC,
      String tncType,
      RequestContext requestContext,
      Map<String, Object> context) {
    String lastAcceptedVersion = "";
    Object tncAcceptedOn = null;
    Map<String, String> allTncAcceptedMap =
        (Map<String, String>) user.get(JsonKey.ALL_TNC_ACCEPTED);
    if (MapUtils.isNotEmpty(allTncAcceptedMap)) {
      String tncAcceptedString = allTncAcceptedMap.getOrDefault(tncType, "");
      Map<String, String> tncAcceptedMap = new HashMap<>();
      try {
        if (StringUtils.isNotBlank(tncAcceptedString)) {
          tncAcceptedMap = mapper.readValue(tncAcceptedString, Map.class);
        }
      } catch (Exception ex) {
        logger.error(requestContext, "Exception occurred while mapping string to map.", ex);
      }
      if (MapUtils.isNotEmpty(tncAcceptedMap)) {
        lastAcceptedVersion = tncAcceptedMap.get(JsonKey.VERSION);
        tncAcceptedOn = tncAcceptedMap.get(JsonKey.TNC_ACCEPTED_ON);
      }
    }
    Response response = new Response();
    Map<String, Object> userMap = new HashMap();
    if (StringUtils.isEmpty(lastAcceptedVersion)
        || !lastAcceptedVersion.equalsIgnoreCase(acceptedTnC)
        || (null == tncAcceptedOn)) {
      logger.info(
          requestContext,
          "UserTnCActor:updateUserTnc: tnc accepted version= "
              + acceptedTnC
              + " accepted on= "
              + user.get(JsonKey.TNC_ACCEPTED_ON)
              + " for userId:"
              + user);
      userMap.put(JsonKey.ID, user.get(JsonKey.ID));
      Map<String, Object> tncAcceptedMap = new HashMap<>();
      tncAcceptedMap.put(JsonKey.VERSION, acceptedTnC);
      tncAcceptedMap.put(JsonKey.TNC_ACCEPTED_ON, ProjectUtil.getFormattedDate());
      try {
        allTncAcceptedMap.put(tncType, mapper.writeValueAsString(tncAcceptedMap));
        userMap.put(JsonKey.ALL_TNC_ACCEPTED, allTncAcceptedMap);
      } catch (Exception ex) {
        logger.error(requestContext, "Exception occurred while mapping", ex);
        ProjectCommonException.throwServerErrorException(ResponseCode.SERVER_ERROR);
      }
      response = userDao.updateUser(userMap, requestContext);
      sender().tell(response, self());
      if (((String) response.get(JsonKey.RESPONSE)).equalsIgnoreCase(JsonKey.SUCCESS)) {
        // In ES this field is getting stored as Map<String, Map<String,String>>
        userMap.put(
            JsonKey.ALL_TNC_ACCEPTED, tncService.convertTncStringToJsonMap(allTncAcceptedMap));
        tncService.syncUserDetails(userMap, requestContext);
      }
    } else {
      response.getResult().put(JsonKey.RESPONSE, JsonKey.SUCCESS);
      sender().tell(response, self());
    }
    tncService.generateTelemetry(userMap, lastAcceptedVersion, context);
  }

  private void updateUserTncConfig(
      Map<String, Object> user,
      String acceptedTnC,
      RequestContext requestContext,
      Map<String, Object> context) {
    String lastAcceptedVersion = (String) user.get(JsonKey.TNC_ACCEPTED_VERSION);
    Object tncAcceptedOn = user.get(JsonKey.TNC_ACCEPTED_ON);
    Response response = new Response();
    Map<String, Object> userMap = new HashMap();
    if (StringUtils.isEmpty(lastAcceptedVersion)
        || !lastAcceptedVersion.equalsIgnoreCase(acceptedTnC)
        || (null == tncAcceptedOn)) {
      logger.info(
          requestContext,
          "UserTnCActor:updateUserTncConfig: tnc accepted version= "
              + acceptedTnC
              + " accepted on= "
              + user.get(JsonKey.TNC_ACCEPTED_ON)
              + " for userId:"
              + user);
      userMap.put(JsonKey.ID, user.get(JsonKey.ID));
      userMap.put(JsonKey.TNC_ACCEPTED_VERSION, acceptedTnC);
      userMap.put(
          JsonKey.TNC_ACCEPTED_ON, new Timestamp(Calendar.getInstance().getTime().getTime()));
      response = userDao.updateUser(userMap, requestContext);
      if (((String) response.get(JsonKey.RESPONSE)).equalsIgnoreCase(JsonKey.SUCCESS)) {
        tncService.syncUserDetails(userMap, requestContext);
      }
      sender().tell(response, self());
    } else {
      response.getResult().put(JsonKey.RESPONSE, JsonKey.SUCCESS);
      sender().tell(response, self());
    }
    tncService.generateTelemetry(userMap, lastAcceptedVersion, context);
  }
}
