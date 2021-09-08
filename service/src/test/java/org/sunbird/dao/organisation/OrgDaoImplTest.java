package org.sunbird.dao.organisation;

import static org.powermock.api.mockito.PowerMockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.cassandraimpl.CassandraOperationImpl;
import org.sunbird.common.CassandraUtil;
import org.sunbird.dao.organisation.impl.OrgDaoImpl;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.keys.JsonKey;
import org.sunbird.model.organisation.OrgTypeEnum;
import org.sunbird.request.RequestContext;
import org.sunbird.response.Response;
import org.sunbird.util.Util;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
  CassandraOperationImpl.class,
  ServiceFactory.class,
  CassandraOperation.class,
  CassandraUtil.class,
  Util.class
})
@PowerMockIgnore({
  "javax.management.*",
  "javax.net.ssl.*",
  "javax.security.*",
  "jdk.internal.reflect.*",
  "javax.crypto.*"
})
public class OrgDaoImplTest {

  private static CassandraOperation cassandraOperation;

  @BeforeClass
  public static void setUp() throws Exception {
    PowerMockito.mockStatic(Util.class);
    cassandraOperation = PowerMockito.mock(CassandraOperation.class);
    PowerMockito.mockStatic(ServiceFactory.class);
    when(ServiceFactory.getInstance()).thenReturn(cassandraOperation);
  }

  @Test
  public void testGetOrgById() {
    try {
      Response response = new Response();
      List<Map<String, Object>> orgList = new ArrayList<>();
      Map<String, Object> map = new HashMap<>();
      map.put(JsonKey.CONTACT_DETAILS, "contact");
      map.put(JsonKey.ID, "contact");
      map.put(JsonKey.ORG_TYPE, OrgTypeEnum.BOARD.getValue());
      map.put(
          JsonKey.ORG_LOCATION,
          "[{\"id\":\"1\",\"type\":\"state\"},{\"id\":\"2\",\"type\":\"district\"}]");
      orgList.add(map);
      response.put(JsonKey.RESPONSE, orgList);
      when(cassandraOperation.getRecordById(
              Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.any()))
          .thenReturn(response);
      OrgDao orgDao = OrgDaoImpl.getInstance();
      Map<String, Object> resp = orgDao.getOrgById("1234567890", null);
      Assert.assertNotNull(resp);

    } catch (Exception e) {
      Assert.assertNotNull(e);
    }
  }

  @Test
  public void getOrgByIds() {
    PowerMockito.when(
            cassandraOperation.getPropertiesValueById(
                Mockito.anyString(),
                Mockito.anyString(),
                Mockito.anyList(),
                Mockito.anyList(),
                Mockito.any(RequestContext.class)))
        .thenReturn(getRecordsByProperty(false));
    List<String> orgIds = new ArrayList<>();
    orgIds.add("id1");
    orgIds.add("id2");
    OrgDao orgDao = OrgDaoImpl.getInstance();
    List<Map<String, Object>> resp = orgDao.getOrgByIds(orgIds, new RequestContext());
    Assert.assertNotNull(resp);
  }

  private Response getRecordsByProperty(boolean empty) {
    Response res = new Response();
    List<Map<String, Object>> list = new ArrayList<>();
    if (!empty) {
      Map<String, Object> map = new HashMap<>();
      map.put(JsonKey.ID, "orgId");
      map.put(JsonKey.IS_DELETED, true);
      map.put(JsonKey.CHANNEL, "channel1");
      map.put(JsonKey.IS_TENANT, true);
      list.add(map);
    }
    res.put(JsonKey.RESPONSE, list);
    return res;
  }

  @Test
  public void testGetOrgByIdWithEmptyResponse() {
    try {
      Response response = new Response();
      List<Map<String, Object>> orgList = new ArrayList<>();
      response.put(JsonKey.RESPONSE, orgList);
      when(cassandraOperation.getRecordById(
              Mockito.anyString(),
              Mockito.anyString(),
              Mockito.anyString(),
              Mockito.any(RequestContext.class)))
          .thenReturn(response);
      OrgDao orgDao = OrgDaoImpl.getInstance();
      Map<String, Object> resp = orgDao.getOrgById("1234567890", null);
      Assert.assertNotNull(resp);

    } catch (Exception e) {
      Assert.assertNotNull(e);
    }
  }
  /*
  @Test
  public void testGetOrgByExternalId() {
    try {
      Response response = new Response();
      List<Map<String, Object>> orgList = new ArrayList<>();
      Map<String, Object> map = new HashMap<>();
      map.put(JsonKey.CONTACT_DETAILS, "contact");
      map.put(JsonKey.ID, "contact");
      orgList.add(map);
      response.put(JsonKey.RESPONSE, orgList);
      when(cassandraOperation.getRecordById(
              Mockito.anyString(),
              Mockito.anyString(),
              Mockito.anyString(),
              Mockito.any(RequestContext.class)))
          .thenReturn(response);

      when(
              orgExternalService.getOrgIdFromOrgExternalIdAndProvider(
                  Mockito.anyString(), Mockito.anyString(), Mockito.any(RequestContext.class)))
          .thenReturn("");
      OrgDao orgDao = OrgDaoImpl.getInstance();
      Map<String, Object> resp = orgDao.getOrgByExternalId("1234567890", "provider", null);
      Assert.assertNotNull(resp);

    } catch (Exception e) {
      Assert.assertNotNull(e);
    }
  }*/
  /*
  @Test
  public void testGetOrgByExternalIdWithEmptyResponse() {
    try {
      cassandraOperation = PowerMockito.mock(CassandraOperation.class);
      PowerMockito.mockStatic(ServiceFactory.class);
      when(ServiceFactory.getInstance()).thenReturn(cassandraOperation);
      Response response = new Response();
      List<Map<String, Object>> orgList = new ArrayList<>();
      response.put(JsonKey.RESPONSE, orgList);
      when(cassandraOperation.getRecordById(
              Mockito.anyString(),
              Mockito.anyString(),
              Mockito.anyString(),
              Mockito.any(RequestContext.class)))
          .thenReturn(response);
      when(
              orgExternalService.getOrgIdFromOrgExternalIdAndProvider(
                  Mockito.anyString(), Mockito.anyString(), Mockito.any(RequestContext.class)))
          .thenReturn("");
      OrgDao orgDao = OrgDaoImpl.getInstance();
      Map<String, Object> resp = orgDao.getOrgByExternalId("1234567890", "provider", null);
      Assert.assertNotNull(resp);

    } catch (Exception e) {
      Assert.assertNotNull(e);
    }
  }*/
}
