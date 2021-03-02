package org.sunbird.user.actors;

import static akka.testkit.JavaTestKit.duration;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.testkit.javadsl.TestKit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.request.Request;
import org.sunbird.common.request.RequestContext;
import org.sunbird.user.dao.UserDao;
import org.sunbird.user.dao.impl.UserDaoImpl;
import org.sunbird.user.dao.impl.UserLookupDaoImpl;

@RunWith(PowerMockRunner.class)
@PrepareForTest({UserLookupDaoImpl.class, UserDao.class, UserDaoImpl.class})
@PowerMockIgnore({
  "javax.management.*",
  "javax.net.ssl.*",
  "javax.security.*",
  "jdk.internal.reflect.*",
  "javax.crypto.*"
})
public class UserLookupActorTest {

  private static ActorSystem system = ActorSystem.create("system");
  private final Props props = Props.create(UserLookupActor.class);

  @Test
  public void getUserKeycloakSearchTestWithId() throws Exception {

    PowerMockito.mockStatic(UserLookupDaoImpl.class);
    UserLookupDaoImpl userLookupDao = PowerMockito.mock(UserLookupDaoImpl.class);
    PowerMockito.when(UserLookupDaoImpl.getInstance()).thenReturn(userLookupDao);
    PowerMockito.when(
            userLookupDao.getRecordByType(
                Mockito.anyString(),
                Mockito.anyString(),
                Mockito.anyBoolean(),
                Mockito.any(RequestContext.class)))
        .thenReturn((List<Map<String, Object>>) getResponse().get(JsonKey.RESPONSE));
    UserDao userDao = PowerMockito.mock(UserDaoImpl.class);
    PowerMockito.mockStatic(UserDaoImpl.class);
    PowerMockito.when(UserDaoImpl.getInstance()).thenReturn(userDao);
    PowerMockito.when(userDao.getUserPropertiesById(Mockito.any(), Mockito.any(), Mockito.any()))
        .thenReturn(getResponse());

    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);
    Request reqObj = new Request();
    reqObj.setOperation(ActorOperations.USER_SEARCH.getValue());
    reqObj.put(JsonKey.KEY, "id");
    reqObj.put(JsonKey.VALUE, "13564");
    List<String> fields = new ArrayList<>();
    fields.add("email");
    fields.add("phone");
    fields.add("id");
    fields.add("userId");
    fields.add("status");
    reqObj.put(JsonKey.FIELDS, fields);
    subject.tell(reqObj, probe.getRef());
    Response res = probe.expectMsgClass(duration("100 second"), Response.class);
    Assert.assertTrue(res != null);
  }

  @Test
  public void getUserKeycloakSearchTestWithKeyValue() throws Exception {
    PowerMockito.mockStatic(UserLookupDaoImpl.class);
    UserLookupDaoImpl userLookupDao = PowerMockito.mock(UserLookupDaoImpl.class);
    PowerMockito.when(UserLookupDaoImpl.getInstance()).thenReturn(userLookupDao);
    PowerMockito.when(
            userLookupDao.getRecordByType(
                Mockito.anyString(),
                Mockito.anyString(),
                Mockito.anyBoolean(),
                Mockito.any(RequestContext.class)))
        .thenReturn((List<Map<String, Object>>) getResponse().get(JsonKey.RESPONSE));

    UserDao userDao = PowerMockito.mock(UserDaoImpl.class);
    PowerMockito.mockStatic(UserDaoImpl.class);
    PowerMockito.when(UserDaoImpl.getInstance()).thenReturn(userDao);
    PowerMockito.when(userDao.getUserPropertiesById(Mockito.any(), Mockito.any(), Mockito.any()))
        .thenReturn(getResponse());

    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);
    Request reqObj = new Request();
    reqObj.setOperation(ActorOperations.USER_SEARCH.getValue());
    reqObj.put(JsonKey.KEY, "email");
    reqObj.put(JsonKey.VALUE, "test@xyz.com");
    List<String> fields = new ArrayList<>();
    fields.add("email");
    fields.add("phone");
    fields.add("id");
    fields.add("userId");
    fields.add("status");
    subject.tell(reqObj, probe.getRef());
    Response res = probe.expectMsgClass(duration("100 second"), Response.class);
    Assert.assertTrue(res != null);
  }

  private Response getResponse() {
    Response response = new Response();
    List<Map<String, Object>> respList = new ArrayList<>();
    Map<String, Object> map = new HashMap<>();
    respList.add(map);
    map.put(JsonKey.ID, "1324566");
    map.put(JsonKey.USER_ID, "1231656");
    response.getResult().put(JsonKey.RESPONSE, respList);
    return response;
  }
}
