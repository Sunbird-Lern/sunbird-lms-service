package org.sunbird.user.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.request.RequestContext;
import org.sunbird.common.responsecode.ResponseCode;
import org.sunbird.user.dao.UserRoleDao;
import org.sunbird.user.dao.impl.UserRoleDaoImpl;
import org.sunbird.user.service.UserRoleService;

public class UserRoleServiceImpl implements UserRoleService {
  private static UserRoleService roleService = null;
  private ObjectMapper mapper = new ObjectMapper();
  UserRoleDao userRoleDao = UserRoleDaoImpl.getInstance();

  public static UserRoleService getInstance() {
    if (roleService == null) {
      roleService = new UserRoleServiceImpl();
    }
    return roleService;
  }
  // Handles roleList in List<String> format
  public List<Map<String, Object>> updateUserRole(Map userRequest, RequestContext context) {
    List<Map<String, Object>> userRoleListResponse = new ArrayList<>();
    List<String> userRolesToInsert = new ArrayList<>();
    List<Map> scopeList = new LinkedList();
    String scopeListString = createRoleScope(scopeList, userRequest);
    userRequest.put(JsonKey.SCOPE_STR, scopeListString);
    String roleOperation = (String) userRequest.get(JsonKey.ROLE_OPERATION);
    if (StringUtils.isNotEmpty(roleOperation) && !JsonKey.CREATE.equals(roleOperation)) {
      List<Map<String, String>> dbUserRoleListToDelete = new ArrayList<>();
      List<Map<String, Object>> dbUserRoleListToUpdate = new ArrayList<>();
      userRolesToInsert =
          createUserRoleInsertUpdateDeleteList(
              userRequest, dbUserRoleListToUpdate, dbUserRoleListToDelete, context);
      // Update existing role scope, if same role is in request
      if (CollectionUtils.isNotEmpty(dbUserRoleListToUpdate)) {
        userRoleDao.updateRoleScope(dbUserRoleListToUpdate, context);
        userRoleListResponse.addAll(dbUserRoleListToUpdate);
      }
      // Delete existing roles of user, if the same is not in request
      if (CollectionUtils.isNotEmpty(dbUserRoleListToDelete)) {
        userRoleDao.deleteUserRole(dbUserRoleListToDelete, context);
      }
    } else {
      userRolesToInsert = (List<String>) userRequest.get(JsonKey.ROLES);
    }
    if (CollectionUtils.isNotEmpty(userRolesToInsert)) {
      List<Map<String, Object>> userRoleListToInsert =
          createUserRoleInsertList(userRolesToInsert, userRequest);
      // Insert roles to DB
      if (CollectionUtils.isNotEmpty(userRoleListToInsert)) {
        userRoleDao.assignUserRole(userRoleListToInsert, context);
        userRoleListResponse.addAll(userRoleListToInsert);
      }
    }
    if (CollectionUtils.isNotEmpty(userRoleListResponse)) {
      userRoleListResponse.forEach(
          map -> {
            map.put(JsonKey.SCOPE, scopeList);
          });
    }
    // Return updated role list to save to ES
    return userRoleListResponse;
  }

  private String createRoleScope(List<Map> scopeList, Map userRequest) {
    Map<String, String> scopeMap = new HashMap<>();
    String organisationId = (String) userRequest.get(JsonKey.ORGANISATION_ID);
    if (JsonKey.CREATE.equals(userRequest.get(JsonKey.ROLE_OPERATION))) {
      scopeMap.put(JsonKey.ORGANISATION_ID, (String) userRequest.get(JsonKey.ROOT_ORG_ID));
    } else {
      scopeMap.put(JsonKey.ORGANISATION_ID, organisationId);
    }
    scopeList.add(scopeMap);
    String scopeListString = convertScopeListToString(scopeList);
    return scopeListString;
  }

  private List<String> createUserRoleInsertUpdateDeleteList(
      Map userRequest,
      List<Map<String, Object>> dbUserRoleListToUpdate,
      List<Map<String, String>> dbUserRoleListToDelete,
      RequestContext context) {
    String userId = (String) userRequest.get(JsonKey.USER_ID);
    List<String> roles = (List<String>) userRequest.get(JsonKey.ROLES);
    List<String> userRolesToInsert = new ArrayList<>();
    // Fetch roles in DB for the user
    List<Map<String, Object>> dbUserRoleList = userRoleDao.getUserRoles(userId, "", context);
    if (CollectionUtils.isNotEmpty(dbUserRoleList)) {
      // Check Db roles to decide whether to update the scope or delete as per the request
      dbUserRoleList.forEach(
          e -> {
            if (roles.stream().filter(d -> d.equals(e.get(JsonKey.ROLE))).count() >= 1) {
              e.put(JsonKey.UPDATED_BY, userRequest.get(JsonKey.REQUESTED_BY));
              e.put(JsonKey.UPDATED_DATE, ProjectUtil.getFormattedDate());
              e.put(JsonKey.SCOPE, userRequest.get(JsonKey.SCOPE_STR));
              dbUserRoleListToUpdate.add(e);
            } else {
              Map userRoleDelete = new HashMap();
              userRoleDelete.put(JsonKey.USER_ID, userId);
              userRoleDelete.put(JsonKey.ROLE, e.get(JsonKey.ROLE));
              dbUserRoleListToDelete.add(userRoleDelete);
            }
          });
      // Fetch roles not in DB from request roles list
      userRolesToInsert =
          roles
              .stream()
              .filter(
                  e ->
                      (dbUserRoleList.stream().filter(d -> d.get(JsonKey.ROLE).equals(e)).count())
                          < 1)
              .collect(Collectors.toList());
    } else {
      // If no records in db, insert the roles in request
      userRolesToInsert = roles;
    }
    return userRolesToInsert;
  }

  private List<Map<String, Object>> createUserRoleInsertList(
      List<String> userRolesToInsert, Map userRequest) {
    List<Map<String, Object>> userRoleListToInsert = new ArrayList<>();
    for (String role : userRolesToInsert) {
      Map userRoleMap = new HashMap();
      userRoleMap.put(JsonKey.ROLE, role);
      userRoleMap.put(JsonKey.USER_ID, userRequest.get(JsonKey.USER_ID));
      userRoleMap.put(JsonKey.SCOPE, userRequest.get(JsonKey.SCOPE_STR));
      userRoleMap.put(JsonKey.CREATED_BY, userRequest.get(JsonKey.REQUESTED_BY));
      userRoleMap.put(JsonKey.CREATED_DATE, ProjectUtil.getFormattedDate());
      userRoleListToInsert.add(userRoleMap);
    }
    return userRoleListToInsert;
  }

  private String convertScopeListToString(List scopeList) {
    String scopeListString = null;
    try {
      scopeListString = mapper.writeValueAsString(scopeList);
    } catch (JsonProcessingException e) {
      throw new ProjectCommonException(
          ResponseCode.roleSaveError.getErrorCode(),
          ResponseCode.roleSaveError.getErrorMessage(),
          ResponseCode.SERVER_ERROR.getResponseCode());
    }
    return scopeListString;
  }

  private List convertScopeStrToList(String scopeStr) {
    List<Map<String, Object>> scopeList = new ArrayList<>();
    try {
      scopeList = mapper.readValue(scopeStr, new ArrayList<Map<String, String>>().getClass());
    } catch (JsonProcessingException ex) {
      throw new ProjectCommonException(
          ResponseCode.roleSaveError.getErrorCode(),
          ResponseCode.roleSaveError.getErrorMessage(),
          ResponseCode.SERVER_ERROR.getResponseCode());
    }
    return scopeList;
  }
  // Handles roleList in List<Map<String, Object>> format
  public List<Map<String, Object>> updateUserRoleV2(Map userRequest, RequestContext context) {
    List<Map<String, Object>> roleList = (List<Map<String, Object>>) userRequest.get(JsonKey.ROLES);
    List<Map<String, Object>> userRoleListToInsert = new ArrayList<>();
    List<Map<String, Object>> userRoleListToUpdate = new ArrayList<>();
    List<Map<String, String>> userRoleListToDelete = new ArrayList<>();
    String userId = (String) userRequest.get(JsonKey.USER_ID);
    // Fetch roles in DB for the user
    List<Map<String, Object>> dbUserRoleList = userRoleDao.getUserRoles(userId, "", context);
    roleList.forEach(
        roleObj -> {
          String roleStr = (String) roleObj.get(JsonKey.ROLE);
          String operation = (String) roleObj.get(JsonKey.OPERATION);
          List<Map<String, Object>> scope =
              ((List<Map<String, Object>>) roleObj.get(JsonKey.SCOPE))
                  .stream()
                  .distinct()
                  .collect(Collectors.toList());
          Map userRoleMap = new HashMap();
          userRoleMap.put(JsonKey.ROLE, roleStr);
          userRoleMap.put(JsonKey.USER_ID, userId);

          Optional<Map<String, Object>> dbRoleRecord =
              dbUserRoleList
                  .stream()
                  .filter(db -> roleStr.equals(db.get(JsonKey.ROLE)))
                  .findFirst();
          if (dbRoleRecord.isEmpty() && JsonKey.ADD.equals(operation)) {
            String scopeListString = convertScopeListToString(scope);
            userRoleMap.put(JsonKey.SCOPE, scopeListString);
            userRoleMap.put(JsonKey.CREATED_BY, userRequest.get(JsonKey.REQUESTED_BY));
            userRoleMap.put(JsonKey.CREATED_DATE, ProjectUtil.getFormattedDate());
            userRoleListToInsert.add(userRoleMap);
            dbUserRoleList.add(userRoleMap);
          } else if (!dbRoleRecord.isEmpty()) {
            String scopeStr = (String) dbRoleRecord.get().get(JsonKey.SCOPE);
            List<Map<String, Object>> dbScope = new ArrayList();
            if (StringUtils.isNotEmpty(scopeStr)) {
              dbScope = convertScopeStrToList(scopeStr);
            }
            if (JsonKey.ADD.equals(operation)) {
              dbScope.forEach(db -> scope.removeIf(sc -> sc.equals(db)));
              dbScope.addAll(scope);
            } else if (JsonKey.REMOVE.equals(operation)) {
              dbScope.removeAll(scope);
            }
            if (dbScope.isEmpty() && JsonKey.REMOVE.equals(operation)) {
              userRoleListToDelete.add(userRoleMap);
              dbUserRoleList.removeIf(db -> roleStr.equals(db.get(JsonKey.ROLE)));
            } else {
              String scopeListString = convertScopeListToString(dbScope);
              userRoleMap.put(JsonKey.SCOPE, scopeListString);
              userRoleMap.put(JsonKey.UPDATED_BY, userRequest.get(JsonKey.REQUESTED_BY));
              userRoleMap.put(JsonKey.UPDATED_DATE, ProjectUtil.getFormattedDate());
              userRoleListToUpdate.add(userRoleMap);
              dbUserRoleList.forEach(
                  db -> {
                    if (roleStr.equals(db.get(JsonKey.ROLE))) {
                      db.put(JsonKey.SCOPE, scopeListString);
                    }
                  });
            }
          }
        });

    // Update existing role scope, if same role is in request
    if (CollectionUtils.isNotEmpty(userRoleListToUpdate)) {
      userRoleDao.updateRoleScope(userRoleListToUpdate, context);
    }
    // Delete existing roles of user
    if (CollectionUtils.isNotEmpty(userRoleListToDelete)) {
      userRoleDao.deleteUserRole(userRoleListToDelete, context);
    }
    // Insert roles to DB
    if (CollectionUtils.isNotEmpty(userRoleListToInsert)) {
      userRoleDao.assignUserRole(userRoleListToInsert, context);
    }
    // Return updated role list to save to ES
    List<Map<String, Object>> roleListResponse = new ArrayList<>();
    if (CollectionUtils.isNotEmpty(dbUserRoleList)) {
      roleListResponse.addAll(dbUserRoleList);
      roleListResponse.forEach(
          map -> {
            map.put(JsonKey.SCOPE, convertScopeStrToList((String) map.get(JsonKey.SCOPE)));
          });
    }
    return roleListResponse;
  }
}
