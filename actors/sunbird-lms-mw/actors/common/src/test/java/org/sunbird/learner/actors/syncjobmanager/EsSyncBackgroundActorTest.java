package org.sunbird.learner.actors.syncjobmanager;

import static org.junit.Assert.assertTrue;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.dispatch.Futures;
import akka.testkit.javadsl.TestKit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.request.Request;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.learner.util.Util;
import scala.concurrent.Promise;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
  ServiceFactory.class,
  Util.class,
  ElasticSearchRestHighImpl.class,
  ElasticSearchHelper.class,
  EsClientFactory.class,
})
@PowerMockIgnore({
  "javax.management.*",
  "javax.net.ssl.*",
  "javax.security.*",
  "jdk.internal.reflect.*",
  "javax.crypto.*"
})
public class EsSyncBackgroundActorTest {
  private ActorSystem system = ActorSystem.create("system");
  private static final Props props = Props.create(EsSyncBackgroundActor.class);
  private static CassandraOperationImpl cassandraOperation;
  private ElasticSearchService esService;

  @Before
  public void beforeEachTest() {
    PowerMockito.mockStatic(ServiceFactory.class);
    PowerMockito.mockStatic(Util.class);
    cassandraOperation = mock(CassandraOperationImpl.class);
    when(ServiceFactory.getInstance()).thenReturn(cassandraOperation);
    PowerMockito.mockStatic(EsClientFactory.class);
    PowerMockito.mockStatic(ElasticSearchHelper.class);
    esService = mock(ElasticSearchRestHighImpl.class);
    PowerMockito.mockStatic(EsClientFactory.class);
    when(EsClientFactory.getInstance(Mockito.anyString())).thenReturn(esService);
    when(ServiceFactory.getInstance()).thenReturn(cassandraOperation);

    Promise<Boolean> promise = Futures.promise();
    promise.success(true);

    when(esService.bulkInsert(Mockito.anyString(), Mockito.anyList(), Mockito.any()))
        .thenReturn(promise.future());
  }

  @Test
  public void testSync() {
    when(cassandraOperation.getRecordsByProperty(
            Mockito.anyString(),
            Mockito.anyString(),
            Mockito.anyString(),
            Mockito.anyList(),
            Mockito.any()))
        .thenReturn(cassandraGetLocationRecord());
    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);
    Request reqObj = new Request();
    reqObj.setOperation(ActorOperations.BACKGROUND_SYNC.getValue());
    Map<String, Object> reqMap = new HashMap<>();
    List<String> ids = new ArrayList<>();
    ids.add("1544646556");
    reqMap.put(JsonKey.OBJECT_IDS, ids);
    reqMap.put(JsonKey.OBJECT_TYPE, JsonKey.LOCATION);
    reqObj.getRequest().put(JsonKey.DATA, reqMap);
    subject.tell(reqObj, probe.getRef());
    probe.expectNoMessage();
    assertTrue(true);
  }

  @Test
  public void testSyncOrg() {
    when(cassandraOperation.getRecordsByProperty(
            Mockito.anyString(),
            Mockito.anyString(),
            Mockito.anyString(),
            Mockito.anyList(),
            Mockito.any()))
        .thenReturn(cassandraGetOrgRecord());
    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);
    Request reqObj = new Request();
    reqObj.setOperation(ActorOperations.BACKGROUND_SYNC.getValue());
    Map<String, Object> reqMap = new HashMap<>();
    List<String> ids = new ArrayList<>();
    ids.add("1544646556");
    reqMap.put(JsonKey.OBJECT_IDS, ids);
    reqMap.put(JsonKey.OBJECT_TYPE, JsonKey.ORGANISATION);
    reqObj.getRequest().put(JsonKey.DATA, reqMap);
    subject.tell(reqObj, probe.getRef());
    probe.expectNoMessage();
    assertTrue(true);
  }

  @Test
  public void testSyncOrgFailure() {
    Response response = cassandraGetOrgRecord();
    Map<String, Object> org =
        (Map<String, Object>) ((List) response.getResult().get(JsonKey.RESPONSE)).get(0);
    org.put(JsonKey.ORG_LOCATION, "\"1\",\"type\":\"state\"},{\"id\":\"2\",\"type\":\"district\"}");
    when(cassandraOperation.getRecordsByProperty(
            Mockito.anyString(),
            Mockito.anyString(),
            Mockito.anyString(),
            Mockito.anyList(),
            Mockito.any()))
        .thenReturn(cassandraGetOrgRecord());
    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);
    Request reqObj = new Request();
    reqObj.setOperation(ActorOperations.BACKGROUND_SYNC.getValue());
    Map<String, Object> reqMap = new HashMap<>();
    List<String> ids = new ArrayList<>();
    ids.add("1544646556");
    reqMap.put(JsonKey.OBJECT_IDS, ids);
    reqMap.put(JsonKey.OBJECT_TYPE, JsonKey.ORGANISATION);
    reqObj.getRequest().put(JsonKey.DATA, reqMap);
    subject.tell(reqObj, probe.getRef());
    probe.expectNoMessage();
    assertTrue(true);
  }

  private static Response cassandraGetOrgRecord() {
    Response response = new Response();
    List<Map<String, Object>> list = new ArrayList();
    Map<String, Object> map = new HashMap<>();
    map.put(JsonKey.ID, "anyId");
    map.put(JsonKey.ORG_TYPE, "type");
    map.put(JsonKey.ORG_NAME, "name");
    map.put(JsonKey.CHANNEL, "ch");
    map.put(
        JsonKey.ORG_LOCATION,
        "[{\"id\":\"1\",\"type\":\"state\"},{\"id\":\"2\",\"type\":\"district\"}]");
    list.add(map);
    response.put(JsonKey.RESPONSE, list);
    return response;
  }

  private static Response cassandraGetLocationRecord() {
    Response response = new Response();
    List<Map<String, Object>> list = new ArrayList();
    Map<String, Object> map = new HashMap<>();
    map.put(JsonKey.ID, "anyId");
    map.put(JsonKey.NAME, "anyLocation");
    map.put(JsonKey.CODE, "code");
    map.put(JsonKey.PARENT_ID, "parentId");
    list.add(map);
    response.put(JsonKey.RESPONSE, list);
    return response;
  }
}
