package org.sunbird.user.dao.impl;

import java.util.*;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.request.RequestContext;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.learner.util.Util;
import org.sunbird.user.dao.UserRoleDao;

public final class UserRoleDaoImpl implements UserRoleDao {

  private static final String TABLE_NAME = JsonKey.USER_ROLES;
  private CassandraOperation cassandraOperation = ServiceFactory.getInstance();

  private static UserRoleDaoImpl instance;

  private UserRoleDaoImpl() {}

  public static UserRoleDao getInstance() {
    if (instance == null) {
      // To make thread safe
      synchronized (UserRoleDaoImpl.class) {
        // check again as multiple threads
        // can reach above step
        if (instance == null) instance = new UserRoleDaoImpl();
      }
    }
    return instance;
  }

  @Override
  public Response assignUserRole(List<Map<String, Object>> userRoleMap, RequestContext context) {
    Response result =
        cassandraOperation.batchInsert(Util.KEY_SPACE_NAME, TABLE_NAME, userRoleMap, context);
    return result;
  }

  @Override
  public Response updateRoleScope(List<Map<String, Object>> userRoleMap, RequestContext context) {
    Response result = null;
    for (Map<String, Object> dataMap : userRoleMap) {

      Map<String, Object> compositeKey = new LinkedHashMap<>(2);
      compositeKey.put(JsonKey.USER_ID, dataMap.remove(JsonKey.USER_ID));
      compositeKey.put(JsonKey.ROLE, dataMap.remove(JsonKey.ROLE));
      result =
          cassandraOperation.updateRecord(
              Util.KEY_SPACE_NAME, TABLE_NAME, dataMap, compositeKey, context);
    }
    return result;
  }

  @Override
  public void deleteUserRole(List<Map<String, String>> userRoleMap, RequestContext context) {
    for (Map<String, String> dataMap : userRoleMap) {
      cassandraOperation.deleteRecord(Util.KEY_SPACE_NAME, TABLE_NAME, dataMap, context);
    }
  }

  @Override
  public List<Map<String, Object>> getUserRoles(
      String userId, String role, RequestContext context) {
    Map compositeKeyMap = new HashMap<String, Object>();
    compositeKeyMap.put(JsonKey.USER_ID, userId);
    if (StringUtils.isNotEmpty(role)) {
      compositeKeyMap.put(JsonKey.ROLE, role);
    }
    Response existingRecord =
        cassandraOperation.getRecordById(Util.KEY_SPACE_NAME, TABLE_NAME, compositeKeyMap, context);
    List<Map<String, Object>> responseList =
        (List<Map<String, Object>>) existingRecord.get(JsonKey.RESPONSE);

    return responseList;
  }
}
