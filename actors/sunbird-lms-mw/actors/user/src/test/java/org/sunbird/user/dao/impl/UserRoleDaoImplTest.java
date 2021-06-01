package org.sunbird.user.dao.impl;

import static org.junit.Assert.*;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

import java.util.*;
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
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.request.RequestContext;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.user.dao.UserRoleDao;

@RunWith(PowerMockRunner.class)
@PrepareForTest({CassandraOperation.class, ServiceFactory.class})
@PowerMockIgnore({
  "javax.management.*",
  "javax.net.ssl.*",
  "javax.security.*",
  "jdk.internal.reflect.*",
  "javax.crypto.*"
})
public class UserRoleDaoImplTest {

  private static CassandraOperation cassandraOperationImpl = null;

  @Before
  public void setUp() {
    PowerMockito.mockStatic(ServiceFactory.class);
    cassandraOperationImpl = mock(CassandraOperationImpl.class);
    when(ServiceFactory.getInstance()).thenReturn(cassandraOperationImpl);
    Response response = new Response();
    when(cassandraOperationImpl.insertRecord(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyMap(), Mockito.any()))
        .thenReturn(response);
    Response getRolesRes = new Response();
    Map<String, Object> roleMap = new HashMap<>();
    roleMap.put("role", "somerole");
    roleMap.put("userId", "someuserId");
    roleMap.put("scope", "[{\"orgnaisationId\":\"someOrgId\"}]");
    List<Map> roleList = new ArrayList<>();
    roleList.add(roleMap);
    getRolesRes.put(JsonKey.RESPONSE, roleList);
    PowerMockito.when(
            cassandraOperationImpl.getRecordById(
                Mockito.any(), Mockito.any(), Mockito.anyMap(), Mockito.any()))
        .thenReturn(getRolesRes);
  }

  @Test
  public void testCreateUserRole() {
    UserRoleDao userRoleDao = UserRoleDaoImpl.getInstance();
    List<Map<String, Object>> res =
        userRoleDao.createUserRole(createUserRoleRequest(), new RequestContext());
    Assert.assertNotNull(res);
  }

  @Test
  public void testGetUserRole() {
    UserRoleDao userRoleDao = UserRoleDaoImpl.getInstance();
    List<Map<String, Object>> res =
        userRoleDao.getUserRoles("someUserId", "someRole", new RequestContext());
    Assert.assertNotNull(res);
  }

  Map createUserRoleRequest() {
    Map<String, Object> userRoleReq = new HashMap<>();
    userRoleReq.put(JsonKey.USER_ID, "ramdomUserId");
    userRoleReq.put(JsonKey.ORGANISATION_ID, "randomOrgID");
    userRoleReq.put(JsonKey.ROLES, Arrays.asList("Admin", "Editor"));
    return userRoleReq;
  }
}
