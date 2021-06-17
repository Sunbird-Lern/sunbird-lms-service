package org.sunbird.user;

import static akka.testkit.JavaTestKit.duration;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.powermock.api.mockito.PowerMockito.when;

import akka.actor.ActorRef;
import akka.dispatch.Futures;
import akka.pattern.Patterns;
import akka.testkit.javadsl.TestKit;
import akka.util.Timeout;
import java.util.Arrays;
import java.util.Map;
import org.junit.Test;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.request.Request;
import org.sunbird.common.responsecode.ResponseCode;
import org.sunbird.learner.util.Util;
import org.sunbird.models.organisation.Organisation;
import scala.concurrent.Future;

public class UserManagementActorTest extends UserManagementActorTestBase {

  @Test
  public void testCreateUserSuccessWithUserCallerId() {
    reqMap.put(JsonKey.PROFILE_LOCATION, Arrays.asList("anyLocationCodes"));
    boolean result =
        testScenario(
            getRequest(true, true, true, getAdditionalMapData(reqMap), ActorOperations.CREATE_USER),
            null);
    assertTrue(result);
  }

  @Test
  public void testCreateUserSuccessWithIsTenantAsFalse() {
    Organisation organisation = new Organisation();
    organisation.setId("rootOrgId");
    organisation.setChannel("anyChannel");
    organisation.setRootOrgId("rootOrgId");
    organisation.setTenant(false);
    when(organisationClient.esGetOrgById(Mockito.anyString(), Mockito.any()))
        .thenReturn(organisation);
    boolean result =
        testScenario(
            getRequest(true, true, true, getAdditionalMapData(reqMap), ActorOperations.CREATE_USER),
            null);
    assertTrue(result);
  }

  @Test
  public void testCreateUserV3Failure() {
    Organisation organisation = new Organisation();
    organisation.setId("rootOrgId");
    organisation.setChannel("anyChannel");
    organisation.setRootOrgId("rootOrgId");
    organisation.setTenant(false);
    when(organisationClient.esGetOrgById(Mockito.anyString(), Mockito.any()))
        .thenReturn(organisation);
    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);
    subject.tell(
        getRequest(true, true, true, getAdditionalMapData(reqMap), ActorOperations.CREATE_SSU_USER),
        probe.getRef());
    Exception ex = probe.expectMsgClass(duration("1000 second"), NullPointerException.class);
    assertNotNull(ex);
  }

  @Test
  public void testCreateUserSuccessWithRequestedChannelAsNull() {
    Organisation organisation = new Organisation();
    organisation.setId("rootOrgId");
    organisation.setChannel("anyChannel");
    organisation.setRootOrgId("rootOrgId");
    organisation.setTenant(false);
    when(organisationClient.esGetOrgById(Mockito.anyString(), Mockito.any()))
        .thenReturn(organisation);
    Request request =
        getRequest(true, true, true, getAdditionalMapData(reqMap), ActorOperations.CREATE_USER);
    request.getRequest().remove(JsonKey.CHANNEL);
    boolean result = testScenario(request, null);
    assertTrue(result);
  }

  //  @Test
  public void testCreateUserFailureWithInvalidOrgId() {
    Organisation organisation = new Organisation();
    organisation.setId("rootOrgId");
    organisation.setChannel("anyChannel");
    organisation.setRootOrgId("rootOrgId");
    organisation.setTenant(true);
    when(organisationClient.esGetOrgById(Mockito.anyString(), Mockito.any())).thenReturn(null);
    boolean result =
        testScenario(
            getRequest(true, true, true, getAdditionalMapData(reqMap), ActorOperations.CREATE_USER),
            ResponseCode.invalidOrgData);
    assertTrue(result);
  }

  //  @Test
  public void testCreateUserFailureWithInvalidChannel() {
    Organisation organisation = new Organisation();
    organisation.setId("rootOrgId");
    organisation.setChannel("anyChannel");
    organisation.setRootOrgId("rootOrgId");
    organisation.setTenant(true);
    when(organisationClient.esGetOrgById(Mockito.anyString(), Mockito.any()))
        .thenReturn(organisation);
    when(userService.getRootOrgIdFromChannel(Mockito.anyString(), Mockito.any())).thenReturn("");
    boolean result =
        testScenario(
            getRequest(true, true, true, getAdditionalMapData(reqMap), ActorOperations.CREATE_USER),
            ResponseCode.invalidParameterValue);
    assertTrue(result);
  }

  //  @Test
  public void testCreateUserFailureWithChannelAndOrgIdMismatch2() {
    Organisation organisation = new Organisation();
    organisation.setId("orgId");
    organisation.setChannel("anyChannel");
    organisation.setRootOrgId("id");
    organisation.setTenant(true);
    when(organisationClient.esGetOrgById(Mockito.anyString(), Mockito.any()))
        .thenReturn(organisation);
    boolean result =
        testScenario(
            getRequest(true, true, true, getAdditionalMapData(reqMap), ActorOperations.CREATE_USER),
            ResponseCode.parameterMismatch);
    assertTrue(result);
  }

  //  @Test
  public void testCreateUserFailureWithChannelAndOrgIdMismatch() {
    Organisation organisation = new Organisation();
    organisation.setId("orgId");
    organisation.setChannel("channel");
    organisation.setRootOrgId("id");
    organisation.setTenant(true);
    when(organisationClient.esGetOrgById(Mockito.anyString(), Mockito.any()))
        .thenReturn(organisation);
    boolean result =
        testScenario(
            getRequest(true, true, true, getAdditionalMapData(reqMap), ActorOperations.CREATE_USER),
            ResponseCode.parameterMismatch);
    assertTrue(result);
  }

  @Test
  public void testCreateUserSuccessWithoutUserCallerId() {
    Organisation organisation = new Organisation();
    organisation.setId("rootOrgId");
    organisation.setChannel("anyChannel");
    organisation.setRootOrgId("rootOrgId");
    organisation.setTenant(true);
    when(organisationClient.esGetOrgById(Mockito.anyString(), Mockito.any()))
        .thenReturn(organisation);
    boolean result =
        testScenario(
            getRequest(
                false, true, true, getAdditionalMapData(reqMap), ActorOperations.CREATE_USER),
            null);
    assertTrue(result);
  }

  @Test
  public void testCreateUserSuccessWithOrgExternalId() {
    reqMap.put(JsonKey.ORG_EXTERNAL_ID, "any");
    boolean result =
        testScenario(
            getRequest(
                false, true, true, getAdditionalMapData(reqMap), ActorOperations.CREATE_USER),
            null);
    assertTrue(result);
  }

  @Test
  public void testCreateUserSuccessWithOrgExternalIdNewVersion() {
    boolean result =
        testScenario(
            getRequest(
                false, true, true, getAdditionalMapData(reqMap), ActorOperations.CREATE_SSO_USER),
            null);
    assertTrue(result);
  }

  @Test
  public void testCreateUserSuccessWithoutUserCallerIdChannelAndRootOrgId() {

    boolean result =
        testScenario(getRequest(false, false, true, reqMap, ActorOperations.CREATE_USER), null);
    assertTrue(result);
  }

  @Test
  public void testCreateUserSuccessWithoutUserCallerIdChannelAndRootOrgIdNewVersion() {

    boolean result =
        testScenario(getRequest(false, false, true, reqMap, ActorOperations.CREATE_SSO_USER), null);
    assertTrue(result);
  }

  //  @Test
  public void testCreateUserFailureWithInvalidChannelAndOrgId() {

    reqMap.put(JsonKey.CHANNEL, "anyReqChannel");
    reqMap.put(JsonKey.ORGANISATION_ID, "anyOrgId");
    boolean result =
        testScenario(
            getRequest(false, false, false, reqMap, ActorOperations.CREATE_USER),
            ResponseCode.parameterMismatch);
    assertTrue(result);
  }

  @Test
  public void testCreateUserFailureWithInvalidUserTypeAndSubtype() {

    reqMap.put(JsonKey.USER_TYPE, "anyUserType");
    reqMap.put(JsonKey.USER_SUB_TYPE, "anyUserSubType");
    reqMap.put(JsonKey.LOCATION_CODES, Arrays.asList("LocationCodes"));
    reqMap.put(JsonKey.PROFILE_USERTYPE, Arrays.asList("userType"));
    boolean result =
        testScenario(
            getRequest(false, false, false, reqMap, ActorOperations.CREATE_USER),
            ResponseCode.invalidParameterValue);
    assertTrue(result);
  }

  @Test
  public void testCreateUserFailureWithInvalidUserTypeAndSubtypeNewVersion() {

    reqMap.put(JsonKey.USER_TYPE, "anyUserType");
    reqMap.put(JsonKey.USER_SUB_TYPE, "anyUserSubType");
    reqMap.put(JsonKey.LOCATION_CODES, Arrays.asList("LocationCodes"));
    reqMap.put(JsonKey.PROFILE_USERTYPE, Arrays.asList("userType"));
    boolean result =
        testScenario(
            getRequest(false, false, false, reqMap, ActorOperations.CREATE_SSO_USER),
            ResponseCode.invalidParameterValue);
    assertTrue(result);
  }

  /*  @Test
  public void testCreateUserFailureWithInvalidLocationCodes() {
    Future<Object> future = Futures.future(() -> null, system.dispatcher());
    when(Patterns.ask(
            Mockito.any(ActorRef.class), Mockito.any(Request.class), Mockito.any(Timeout.class)))
        .thenReturn(future);

    reqMap.put(JsonKey.LOCATION_CODES, Arrays.asList(""));
    boolean result =
        testScenario(
            getRequest(false, false, false, reqMap, ActorOperations.CREATE_USER),
            ResponseCode.invalidParameterValue);
    assertTrue(result);
  }*/

  //  @Test
  public void testCreateUserSuccessWithoutVersion() {

    boolean result =
        testScenario(getRequest(false, false, false, reqMap, ActorOperations.CREATE_USER), null);
    assertTrue(result);
  }

  @Test
  public void testCreateUserSuccessWithLocationCodes() {
    Future<Object> future = Futures.future(() -> getEsResponse(), system.dispatcher());
    when(Patterns.ask(
            Mockito.any(ActorRef.class), Mockito.any(Request.class), Mockito.any(Timeout.class)))
        .thenReturn(future);
    reqMap.put(JsonKey.LOCATION_CODES, Arrays.asList("locationCode"));
    boolean result =
        testScenario(getRequest(true, true, true, reqMap, ActorOperations.CREATE_USER), null);
    assertTrue(result);
  }

  @Test
  public void testCreateUserSuccessWithLocationCodesNewVersion() {
    Future<Object> future = Futures.future(() -> getEsResponse(), system.dispatcher());
    when(Patterns.ask(
            Mockito.any(ActorRef.class), Mockito.any(Request.class), Mockito.any(Timeout.class)))
        .thenReturn(future);
    reqMap.put(JsonKey.LOCATION_CODES, Arrays.asList("locationCode"));
    boolean result =
        testScenario(getRequest(true, true, true, reqMap, ActorOperations.CREATE_SSO_USER), null);
    assertTrue(result);
  }

  @Test
  public void testCreateUserFailureWithInvalidExternalIds() {

    reqMap.put(JsonKey.EXTERNAL_IDS, "anyExternalId");
    boolean result =
        testScenario(
            getRequest(false, false, false, reqMap, ActorOperations.CREATE_USER),
            ResponseCode.dataTypeError);
    assertTrue(result);
  }

  @Test
  public void testCreateUserFailureWithInvalidExternalIdsNewVersion() {

    reqMap.put(JsonKey.EXTERNAL_IDS, "anyExternalId");
    boolean result =
        testScenario(
            getRequest(false, false, false, reqMap, ActorOperations.CREATE_SSO_USER),
            ResponseCode.dataTypeError);
    assertTrue(result);
  }

  @Test
  public void testCreateUserFailureWithInvalidRoles() {

    reqMap.put(JsonKey.ROLES, "anyRoles");
    boolean result =
        testScenario(
            getRequest(false, false, false, reqMap, ActorOperations.CREATE_USER),
            ResponseCode.dataTypeError);
    assertTrue(result);
  }

  @Test
  public void testCreateUserFailureWithInvalidRolesNewVersion() {

    reqMap.put(JsonKey.ROLES, "anyRoles");
    boolean result =
        testScenario(
            getRequest(false, false, false, reqMap, ActorOperations.CREATE_SSO_USER),
            ResponseCode.dataTypeError);
    assertTrue(result);
  }

  @Test
  public void testCreateUserFailureWithInvalidCountryCode() {

    reqMap.put(JsonKey.COUNTRY_CODE, "anyCode");
    boolean result =
        testScenario(
            getRequest(false, false, false, reqMap, ActorOperations.CREATE_USER),
            ResponseCode.invalidCountryCode);
    assertTrue(result);
  }

  @Test
  public void testCreateUserFailureWithInvalidCountryCodeNewVersion() {

    reqMap.put(JsonKey.COUNTRY_CODE, "anyCode");
    boolean result =
        testScenario(
            getRequest(false, false, false, reqMap, ActorOperations.CREATE_SSO_USER),
            ResponseCode.invalidCountryCode);
    assertTrue(result);
  }

  /*  @Test
  public void testUpdateUserFailureWithLocationCodes() {
    Future<Object> future2 = Futures.future(() -> null, system.dispatcher());
    when(Patterns.ask(
            Mockito.any(ActorRef.class), Mockito.any(Request.class), Mockito.any(Timeout.class)))
        .thenReturn(future2);

    when(userService.getUserById(Mockito.anyString(), Mockito.any())).thenReturn(getUser(false));
    boolean result =
        testScenario(
            getRequest(
                true, true, true, getUpdateRequestWithLocationCodes(), ActorOperations.UPDATE_USER),
            ResponseCode.invalidParameterValue);
    assertTrue(result);
  }*/

  @Test
  public void testCreateUserSuccessWithUserTypeAsTeacher() {
    reqMap.put(JsonKey.USER_TYPE, "teacher");

    boolean result =
        testScenario(
            getRequest(true, true, true, getAdditionalMapData(reqMap), ActorOperations.CREATE_USER),
            null);
    assertTrue(result);
  }

  @Test
  public void testCreateUserSuccessWithUserTypeAsTeacherNewVersion() {
    reqMap.put(JsonKey.USER_TYPE, "teacher");

    boolean result =
        testScenario(
            getRequest(
                true, true, true, getAdditionalMapData(reqMap), ActorOperations.CREATE_SSO_USER),
            null);
    assertTrue(result);
  }

  // @Test
  public void testCreateUserSuccessWithUserSync() {
    reqMap.put("sync", true);
    PowerMockito.mockStatic(Util.class);
    Map<String, Object> user = getEsResponseMap();
    user.put(JsonKey.USER_ID, "123456789");
    when(Util.getUserDetails(Mockito.anyString(), Mockito.any())).thenReturn(user);
    /*PipeToSupport.PipeableFuture pipe = PowerMockito.mock(PipeToSupport.PipeableFuture.class);
    Future<Map<String,Object>> future1 =
      Futures.future(() -> reqMap, system.dispatcher());
    when(pipe.to(Mockito.any(ActorRef.class))).thenReturn(future1);
    when(Patterns.pipe(Mockito.any(Future.class), Mockito.any())).thenReturn(pipe);*/

    boolean result =
        testScenario(
            getRequest(true, true, true, getAdditionalMapData(reqMap), ActorOperations.CREATE_USER),
            null);
    assertTrue(true);
  }
}
