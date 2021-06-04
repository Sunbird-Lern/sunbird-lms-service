package org.sunbird.user.service;

import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.cassandraimpl.CassandraOperationImpl;
import org.sunbird.common.ElasticSearchRestHighImpl;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.factory.EsClientFactory;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.request.Request;
import org.sunbird.common.responsecode.ResponseCode;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.learner.util.DataCacheHandler;
import org.sunbird.user.util.UserUtil;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
  UserUtil.class,
  ServiceFactory.class,
  CassandraOperationImpl.class,
  EsClientFactory.class,
  ElasticSearchRestHighImpl.class,
  DataCacheHandler.class
})
@PowerMockIgnore({
  "javax.management.*",
  "javax.net.ssl.*",
  "javax.security.*",
  "jdk.internal.reflect.*",
  "javax.crypto.*"
})
public class UserTncServiceTest {
  private String tncConfig =
      "{\"latestVersion\":\"V1\",\"v1\":{\"url\":\"http://dev/terms.html\"},\"v2\":{\"url\":\"http://dev/terms.html\"},\"v4\":{\"url\":\"http://dev/terms.html\"}}";
  private String groupsConfig =
      "{\"latestVersion\":\"V1\",\"v1\":{\"url\":\"http://dev/terms.html\"},\"v2\":{\"url\":\"http://dev/terms.html\"},\"v4\":{\"url\":\"http://dev/terms.html\"}}";

  private String orgAdminTnc =
      "{\"latestVersion\":\"V1\",\"v1\":{\"url\":\"http://dev/terms.html\"},\"v2\":{\"url\":\"http://dev/terms.html\"},\"v4\":{\"url\":\"http://dev/terms.html\"}}";

  @Before
  public void beforeEachTest() {
    PowerMockito.mockStatic(DataCacheHandler.class);
    PowerMockito.mockStatic(ServiceFactory.class);
    Map<String, String> config = new HashMap<>();
    config.put(JsonKey.TNC_CONFIG, tncConfig);
    config.put("groups", groupsConfig);
    config.put("orgAdminTnc", orgAdminTnc);
    when(DataCacheHandler.getConfigSettings()).thenReturn(config);
  }

  @Test
  public void getUserByIdTest() {
    CassandraOperation cassandraOperationImpl = mock(CassandraOperation.class);
    when(ServiceFactory.getInstance()).thenReturn(cassandraOperationImpl);
    Response response = new Response();
    List<Map<String, Object>> resp = new ArrayList<>();
    Map<String, Object> userList = new HashMap<>();
    userList.put(JsonKey.USER_ID, "1234");
    userList.put(JsonKey.IS_DELETED, false);
    resp.add(userList);
    response.put(JsonKey.RESPONSE, resp);
    when(cassandraOperationImpl.getRecordById(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.any()))
        .thenReturn(response);
    UserTncService tncService = new UserTncService();

    Map<String, Object> user = tncService.getUserById("1234", null);
    Assert.assertNotNull(user);
  }

  @Test
  public void getUserByIdForLockedAccountTest() {
    CassandraOperation cassandraOperationImpl = mock(CassandraOperation.class);
    when(ServiceFactory.getInstance()).thenReturn(cassandraOperationImpl);
    Response response = new Response();
    List<Map<String, Object>> resp = new ArrayList<>();
    Map<String, Object> userList = new HashMap<>();
    userList.put(JsonKey.USER_ID, "1234");
    userList.put(JsonKey.IS_DELETED, true);
    resp.add(userList);
    response.put(JsonKey.RESPONSE, resp);
    when(cassandraOperationImpl.getRecordById(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.any()))
        .thenReturn(response);
    UserTncService tncService = new UserTncService();
    try {
      tncService.getUserById("1234", null);
    } catch (ProjectCommonException ex) {
      Assert.assertEquals(ResponseCode.userAccountlocked.getErrorCode(), ex.getCode());
    }
  }

  @Test
  public void getUserByIdForEmptyResultTest() {
    CassandraOperation cassandraOperationImpl = mock(CassandraOperation.class);
    when(ServiceFactory.getInstance()).thenReturn(cassandraOperationImpl);
    Response response = new Response();
    when(cassandraOperationImpl.getRecordById(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.any()))
        .thenReturn(response);
    UserTncService tncService = new UserTncService();
    try {
      tncService.getUserById("1234", null);
    } catch (ProjectCommonException ex) {
      Assert.assertEquals(ResponseCode.userNotFound.getErrorCode(), ex.getCode());
    }
  }

  @Test
  public void validateLatestTncVersionTest() {
    Request request = new Request();
    request.getRequest().put(JsonKey.VERSION, "v2");
    UserTncService tncService = new UserTncService();
    try {
      tncService.validateLatestTncVersion(request, "groups");
    } catch (ProjectCommonException ex) {
      Assert.assertEquals(ResponseCode.invalidParameterValue.getErrorCode(), ex.getCode());
    }
  }

  @Test
  public void acceptOrgAdminTncTest() {
    CassandraOperation cassandraOperationImpl = mock(CassandraOperation.class);
    when(ServiceFactory.getInstance()).thenReturn(cassandraOperationImpl);
    when(cassandraOperationImpl.getRecordById(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyMap(), Mockito.any()))
        .thenReturn(getCassandraUserRoleAdminResponse());
    UserTncService tncService = new UserTncService();
    Map<String, Object> searchMap = userOrgData();
    try {
      tncService.validateRoleForTnc(null, "orgAdminTnc", searchMap);
    } catch (ProjectCommonException ex) {
      Assert.assertEquals(ResponseCode.invalidParameterValue.getErrorCode(), ex.getCode());
    }
  }

  @Test
  public void validateOrgAdminTncTest() {
    CassandraOperation cassandraOperationImpl = mock(CassandraOperation.class);
    when(ServiceFactory.getInstance()).thenReturn(cassandraOperationImpl);
    when(cassandraOperationImpl.getRecordById(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyMap(), Mockito.any()))
        .thenReturn(getCassandraUserRoleResponse());
    UserTncService tncService = new UserTncService();
    Map<String, Object> searchMap = userOrgData();
    try {
      tncService.validateRoleForTnc(null, "orgAdminTnc", searchMap);
    } catch (ProjectCommonException ex) {
      Assert.assertEquals(ResponseCode.invalidParameterValue.getErrorCode(), ex.getCode());
    }
  }

  @Test
  public void reportViewerTncTest() {
    CassandraOperation cassandraOperationImpl = mock(CassandraOperation.class);
    when(ServiceFactory.getInstance()).thenReturn(cassandraOperationImpl);
    when(cassandraOperationImpl.getRecordById(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyMap(), Mockito.any()))
        .thenReturn(getCassandraUserRoleResponse());
    UserTncService tncService = new UserTncService();
    Map<String, Object> searchMap = userOrgData();
    try {
      tncService.validateRoleForTnc(null, "reportViewerTnc", searchMap);
    } catch (ProjectCommonException ex) {
      Assert.assertEquals(ResponseCode.invalidParameterValue.getErrorCode(), ex.getCode());
    }
  }

  private static Response getCassandraUserRoleResponse() {
    Response response = new Response();
    List<Map<String, Object>> list = new ArrayList<>();
    Map<String, Object> orgMap = new HashMap<>();
    orgMap.put(JsonKey.USER_ID, "1234");
    orgMap.put(JsonKey.ROLE, "PUBLIC");
    list.add(orgMap);
    response.put(JsonKey.RESPONSE, list);
    return response;
  }

  private static Response getCassandraUserRoleAdminResponse() {
    Response response = new Response();
    List<Map<String, Object>> list = new ArrayList<>();
    Map<String, Object> orgMap = new HashMap<>();
    orgMap.put(JsonKey.USER_ID, "1234");
    orgMap.put(JsonKey.ROLE, "ORG_ADMIN");
    orgMap.put(JsonKey.SCOPE, "[{\"organisationId\":\"4567\"}]");
    list.add(orgMap);
    response.put(JsonKey.RESPONSE, list);
    return response;
  }

  private Map<String, Object> userOrgData() {
    Map<String, Object> searchMap = new LinkedHashMap<>(2);
    searchMap.put(JsonKey.USER_ID, "1234");
    searchMap.put(JsonKey.ORGANISATION_ID, "4567");
    searchMap.put(JsonKey.ROOT_ORG_ID, "4567");
    return searchMap;
  }
}
