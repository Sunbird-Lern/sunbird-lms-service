package org.sunbird.util;

import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

import java.util.HashMap;
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
import org.sunbird.cassandraimpl.CassandraOperationImpl;
import org.sunbird.common.ElasticSearchHelper;
import org.sunbird.common.ElasticSearchRestHighImpl;
import org.sunbird.common.factory.EsClientFactory;
import org.sunbird.common.inf.ElasticSearchService;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.keys.JsonKey;
import org.sunbird.notification.sms.provider.ISmsProvider;
import org.sunbird.notification.utils.SMSFactory;
import org.sunbird.request.RequestContext;
import org.sunbird.response.Response;
import org.sunbird.service.organisation.OrgService;
import org.sunbird.service.organisation.impl.OrgServiceImpl;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
  ServiceFactory.class,
  EsClientFactory.class,
  ElasticSearchRestHighImpl.class,
  ElasticSearchHelper.class,
  CassandraOperationImpl.class,
  SMSFactory.class,
  ISmsProvider.class
})
@PowerMockIgnore({
  "javax.management.*",
  "javax.net.ssl.*",
  "javax.security.*",
  "jdk.internal.reflect.*",
  "javax.crypto.*"
})
public class UtilTest {
  private static CassandraOperationImpl cassandraOperationImpl;
  private static ElasticSearchService esService;
  private OrgService orgService = OrgServiceImpl.getInstance();

  @Before
  public void beforeEachTest() {
    PowerMockito.mockStatic(ServiceFactory.class);
    cassandraOperationImpl = mock(CassandraOperationImpl.class);
    PowerMockito.mockStatic(EsClientFactory.class);
    esService = mock(ElasticSearchRestHighImpl.class);
    when(EsClientFactory.getInstance(Mockito.anyString())).thenReturn(esService);
  }

  @Test
  public void testRegisterChannel() {
    Map<String, Object> map = new HashMap<>();
    map.put(JsonKey.CHANNEL, "ch");
    map.put(JsonKey.DESCRIPTION, "desc");
    map.put(JsonKey.ID, "id");
    Boolean bool = orgService.registerChannel(map, new RequestContext());
    Assert.assertNotNull(bool);
  }

  @Test
  public void testUpdateChannel() {
    Map<String, Object> map = new HashMap<>();
    map.put(JsonKey.CHANNEL, "ch");
    map.put(JsonKey.DESCRIPTION, "desc");
    map.put(JsonKey.ID, "id");
    Boolean bool = orgService.updateChannel(map, new RequestContext());
    Assert.assertNotNull(bool);
  }

  @Test
  public void testRegisterUserToOrg() {
    Map<String, Object> map = new HashMap<>();
    map.put(JsonKey.ID, "123456");
    map.put(JsonKey.USER_ID, "123456");
    map.put(JsonKey.ORGANISATION_ID, "123456");
    map.put(JsonKey.IS_DELETED, false);
    map.put(JsonKey.ASSOCIATION_TYPE, "1");
    Response response = new Response();
    when(cassandraOperationImpl.insertRecord(JsonKey.SUNBIRD, "user_organisation", map, null))
        .thenReturn(response);
    Assert.assertNotNull(response);
  }

  public static Map<String, Object> getEsResponseMap() {
    Map<String, Object> map = new HashMap<>();
    map.put(JsonKey.IS_TENANT, true);
    map.put(JsonKey.ID, "rootOrgId");
    map.put(JsonKey.CHANNEL, "anyChannel");
    return map;
  }

  public static Map<String, Map<String, Object>> getEs2ResponseMap() {
    Map<String, Map<String, Object>> map2 = new HashMap<>();
    Map<String, Object> map = new HashMap<>();
    map.put(JsonKey.IS_TENANT, true);
    map.put(JsonKey.ID, "rootOrgId");
    map.put(JsonKey.CHANNEL, "anyChannel");
    map2.put(JsonKey.RESPONSE, map);
    return map2;
  }
}
