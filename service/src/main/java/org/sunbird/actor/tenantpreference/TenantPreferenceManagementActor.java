package org.sunbird.actor.tenantpreference;

import org.sunbird.actor.core.BaseActor;
import org.sunbird.actor.router.ActorConfig;
import org.sunbird.keys.JsonKey;
import org.sunbird.operations.ActorOperations;
import org.sunbird.request.Request;
import org.sunbird.request.RequestContext;
import org.sunbird.response.Response;
import org.sunbird.service.tenantpreference.TenantPreferenceService;

import java.util.Map;

@ActorConfig(
  tasks = {
    "createTanentPreference",
    "updateTenantPreference",
    "getTenantPreference",
  },
  asyncTasks = {}
)
public class TenantPreferenceManagementActor extends BaseActor {

  private TenantPreferenceService preferenceService = new TenantPreferenceService();

  @Override
  public void onReceive(Request request) throws Throwable {
    if (request
        .getOperation()
        .equalsIgnoreCase(ActorOperations.CREATE_TENANT_PREFERENCE.getValue())) {
      createTenantPreference(request);
    } else if (request
        .getOperation()
        .equalsIgnoreCase(ActorOperations.UPDATE_TENANT_PREFERENCE.getValue())) {
      updateTenantPreference(request);
    } else if (request
        .getOperation()
        .equalsIgnoreCase(ActorOperations.GET_TENANT_PREFERENCE.getValue())) {
      getTenantPreference(request);
    } else {
      onReceiveUnsupportedOperation(request.getOperation());
    }
  }

  /**
   * Method to get Tenant preference of the given root org id .
   *
   * @param actorMessage
   */
  private void getTenantPreference(Request actorMessage) {
    RequestContext context = actorMessage.getRequestContext();
    String orgId = (String) actorMessage.getRequest().get(JsonKey.ORG_ID);
    logger.debug(
        context, "TenantPreferenceManagementActor:getTenantPreference called for org: " + orgId);
    String key = (String) actorMessage.getRequest().get(JsonKey.KEY);
    Map<String, Object> orgPref = preferenceService.validateAndGetTenantPreferencesById(orgId, key, JsonKey.GET, context);
    Response finalResponse = new Response();
    finalResponse.getResult().put(JsonKey.RESPONSE, orgPref);
    sender().tell(finalResponse, self());
  }
  /**
   * Method to update the Tenant preference on basis of id or (role and org id).
   *
   * @param actorMessage
   */
  private void updateTenantPreference(Request actorMessage) {
    RequestContext context = actorMessage.getRequestContext();
    Map<String, Object> req = actorMessage.getRequest();
    String orgId = (String) req.get(JsonKey.ORG_ID);
    String key = (String) req.get(JsonKey.KEY);
    logger.debug(
        context,
        "TenantPreferenceManagementActor:updateTenantPreference called for org: "
            + orgId
            + "key "
            + key);
    Response finalResponse = new Response();
    preferenceService.validateAndGetTenantPreferencesById(orgId, key, JsonKey.UPDATE, context);
    Map<String,Object> data = (Map<String, Object>) req.get(JsonKey.DATA);
    String updatedBy = (String) actorMessage.getContext().get(JsonKey.REQUESTED_BY);
    preferenceService.updatePreference(orgId, key, data, updatedBy, context);

    finalResponse.getResult().put(JsonKey.RESPONSE, JsonKey.SUCCESS);
    sender().tell(finalResponse, self());
  }

  /**
   * Method to create tenant preference for an org , if already exists it will not create new one.
   *
   * @param actorMessage
   */
  private void createTenantPreference(Request actorMessage) {
    RequestContext context = actorMessage.getRequestContext();
    Map<String, Object> req = actorMessage.getRequest();
    String orgId = (String) req.get(JsonKey.ORG_ID);
    String key = (String) req.get(JsonKey.KEY);
    logger.debug(
        context, "TenantPreferenceManagementActor:createTenantPreference called for org: " + orgId);
    preferenceService.validateAndGetTenantPreferencesById(orgId, key, JsonKey.CREATE, context);
    Map<String,Object> data = (Map<String, Object>) req.get(JsonKey.DATA);
    String requestedBy = (String) actorMessage.getContext().get(JsonKey.REQUESTED_BY);
    preferenceService.createPreference(orgId, key, data, requestedBy, context);

    Response finalResponse = new Response();
    finalResponse.getResult().put(JsonKey.ORG_ID, orgId);
    finalResponse.getResult().put(JsonKey.KEY, key);
    finalResponse.getResult().put(JsonKey.RESPONSE, JsonKey.SUCCESS);
    sender().tell(finalResponse, self());
  }
}
