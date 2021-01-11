package org.sunbird.user.dao.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.request.RequestContext;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.learner.util.Util;
import org.sunbird.models.user.org.UserOrg;
import org.sunbird.user.dao.UserOrgDao;

public final class UserOrgDaoImpl implements UserOrgDao {

  private static final String TABLE_NAME = JsonKey.USER_ORG;
  private CassandraOperation cassandraOperation = ServiceFactory.getInstance();

  private ObjectMapper mapper = new ObjectMapper();

  private UserOrgDaoImpl() {}

  private static class LazyInitializer {
    private static UserOrgDao INSTANCE = new UserOrgDaoImpl();
  }

  public static UserOrgDao getInstance() {
    return LazyInitializer.INSTANCE;
  }

  @Override
  public Response updateUserOrg(UserOrg userOrg, RequestContext context) {
    Map<String, Object> compositeKey = new LinkedHashMap<>(2);
    Map<String, Object> request = mapper.convertValue(userOrg, Map.class);
    compositeKey.put(JsonKey.USER_ID, request.remove(JsonKey.USER_ID));
    compositeKey.put(JsonKey.ORGANISATION_ID, request.remove(JsonKey.ORGANISATION_ID));
    return cassandraOperation.updateRecord(
        Util.KEY_SPACE_NAME, TABLE_NAME, request, compositeKey, context);
  }

  @Override
  public Response createUserOrg(UserOrg userOrg, RequestContext context) {
    return cassandraOperation.insertRecord(
        Util.KEY_SPACE_NAME, TABLE_NAME, mapper.convertValue(userOrg, Map.class), context);
  }

  @Override
  public Response getUserOrgListByUserId(String userId, RequestContext context) {
    Map<String, Object> compositeKey = new LinkedHashMap<>(2);
    compositeKey.put(JsonKey.USER_ID, userId);
    return cassandraOperation.getRecordById(Util.KEY_SPACE_NAME, TABLE_NAME, compositeKey, context);
  }

  @Override
  public Response getUserOrgDetails(String userId, String organisationId, RequestContext context) {
    Map<String, Object> searchMap = new LinkedHashMap<>(2);
    searchMap.put(JsonKey.USER_ID, userId);
    if (StringUtils.isNotEmpty(organisationId)) {
      searchMap.put(JsonKey.ORGANISATION_ID, organisationId);
    }
    Response res =
        cassandraOperation.getRecordsByCompositeKey(
            JsonKey.SUNBIRD, JsonKey.USER_ORG, searchMap, context);
    return res;
  }
}
