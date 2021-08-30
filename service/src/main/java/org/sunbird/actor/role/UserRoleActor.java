package org.sunbird.actor.role;

import org.apache.commons.lang3.StringUtils;
import org.sunbird.actor.router.ActorConfig;
import org.sunbird.actor.user.UserBaseActor;
import org.sunbird.keys.JsonKey;
import org.sunbird.operations.ActorOperations;
import org.sunbird.request.Request;
import org.sunbird.request.RequestContext;
import org.sunbird.response.Response;
import org.sunbird.service.organisation.OrgService;
import org.sunbird.service.organisation.impl.OrgServiceImpl;
import org.sunbird.service.role.RoleService;
import org.sunbird.service.user.UserRoleService;
import org.sunbird.service.user.impl.UserRoleServiceImpl;
import org.sunbird.telemetry.dto.TelemetryEnvKey;
import org.sunbird.util.DataCacheHandler;
import org.sunbird.util.Util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@ActorConfig(
  tasks = {"getRoles", "assignRoles", "assignRolesV2", "getUserRolesById"},
  asyncTasks = {},
  dispatcher = "most-used-two-dispatcher"
)
public class UserRoleActor extends UserBaseActor {

  private UserRoleService userRoleService = UserRoleServiceImpl.getInstance();
  private OrgService orgService = OrgServiceImpl.getInstance();

  @Override
  public void onReceive(Request request) throws Throwable {
    Util.initializeContext(request, TelemetryEnvKey.USER);
    String operation = request.getOperation();

    switch (operation) {
      case "getRoles":
        getRoles(request.getRequestContext());
        break;

      case "assignRoles":

      case "assignRolesV2":
        assignRoles(request);
        break;

      case "getUserRolesById":
        getUserRolesById(request);
        break;  

      default:
        onReceiveUnsupportedOperation("UserRoleActor");
    }
  }

  private void getUserRolesById(Request request) {
    Map<String, Object> requestMap = request.getRequest();
    String userId = (String) requestMap.get(JsonKey.USER_ID);
    List<Map<String,Object>> userRoles = userRoleService.getUserRoles(userId, request.getRequestContext());
    String requestedFields = (String) request.getContext().get(JsonKey.FIELDS);
    if (StringUtils.isNotBlank(requestedFields) && requestedFields.contains(JsonKey.ORG_NAME)) {
      Set<String> orgIds = new HashSet<>();
      userRoles.stream().forEach(
        userRole ->
          ((List<Map<String, String>>)userRole.get(JsonKey.SCOPE))
            .stream()
            .forEach(scope -> orgIds.add(scope.get(JsonKey.ORGANISATION_ID))));
      List<Map<String,Object>> orgList = orgService.getOrgByIds(new ArrayList<>(orgIds), request.getRequestContext());
      Map<String, String> orgIdMap = new HashMap<>();
      orgList.stream().forEach(org -> orgIdMap.put((String)org.get(JsonKey.ID), (String) org.get(JsonKey.ORG_NAME)));
      userRoles.stream().forEach(
        userRole ->
          ((List<Map<String, String>>)userRole.get(JsonKey.SCOPE))
            .stream()
            .forEach(scope ->
            {
              String orgId = scope.get(JsonKey.ORGANISATION_ID);
              if (StringUtils.isNotBlank(orgId) && orgIdMap.containsKey(orgId)) {
                scope.put(JsonKey.ORG_NAME,orgIdMap.get(orgId));
              }
            }));
    }
    Response response = new Response();
    response.put(JsonKey.ROLES,userRoles);
    sender().tell(response, self());
  }

  private void getRoles(RequestContext context) {
    Response response = DataCacheHandler.getRoleResponse();
    if (response == null) {
      response = new RoleService().getUserRoles(context);
      DataCacheHandler.setRoleResponse(response);
    }
    sender().tell(response, self());
  }

  @SuppressWarnings("unchecked")
  private void assignRoles(Request actorMessage) {
    List<Map<String, Object>> userRolesList;

    Map<String, Object> requestMap = actorMessage.getRequest();
    requestMap.put(JsonKey.REQUESTED_BY, actorMessage.getContext().get(JsonKey.USER_ID));

    if (actorMessage.getOperation().equals(ActorOperations.ASSIGN_ROLES.getValue())) {
      requestMap.put(JsonKey.ROLE_OPERATION, "assignRole");
      List<String> roles = (List<String>) requestMap.get(JsonKey.ROLES);
      RoleService.validateRoles(roles);
      userRolesList = userRoleService.updateUserRole(requestMap, actorMessage.getRequestContext());
    } else {
      List<Map<String, Object>> roleList =
          (List<Map<String, Object>>) requestMap.get(JsonKey.ROLES);
      RoleService.validateRolesV2(roleList);
      userRolesList =
          userRoleService.updateUserRoleV2(requestMap, actorMessage.getRequestContext());
    }
    Response response = new Response();
    response.put(JsonKey.RESPONSE, JsonKey.SUCCESS);

    sender().tell(response, self());
    syncUserRoles(
        JsonKey.USER,
        (String) requestMap.get(JsonKey.USER_ID),
        userRolesList,
        actorMessage.getRequestContext());
    generateTelemetryEvent(
        requestMap,
        (String) requestMap.get(JsonKey.USER_ID),
        "userLevel",
        actorMessage.getContext());
  }

  private void syncUserRoles(
      String type, String userId, List<Map<String, Object>> userRolesList, RequestContext context) {
    Request request = new Request();
    request.setRequestContext(context);
    request.setOperation(ActorOperations.UPDATE_USER_ROLES_ES.getValue());
    request.getRequest().put(JsonKey.TYPE, type);
    request.getRequest().put(JsonKey.USER_ID, userId);
    request.getRequest().put(JsonKey.ROLES, userRolesList);
    logger.debug(context, "UserRoleActor:syncUserRoles: Syncing to ES");
    try {
      tellToAnother(request);
    } catch (Exception ex) {
      logger.error(
          context,
          "UserRoleActor:syncUserRoles: Exception occurred with error message = " + ex.getMessage(),
          ex);
    }
  }
}
