package org.sunbird.user.actors;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
import org.sunbird.actor.core.BaseActor;
import org.sunbird.actor.router.ActorConfig;
import org.sunbird.common.ElasticSearchHelper;
import org.sunbird.common.factory.EsClientFactory;
import org.sunbird.common.inf.ElasticSearchService;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.request.Request;
import org.sunbird.common.request.RequestContext;
import org.sunbird.models.user.User;
import org.sunbird.user.util.UserUtil;
import scala.concurrent.Future;

@ActorConfig(
  tasks = {"upsertUserDetailsToES", "upsertUserOrgDetailsToES"},
  asyncTasks = {"upsertUserDetailsToES", "upsertUserOrgDetailsToES"},
  dispatcher = "most-used-two-dispatcher"
)
public class UserBackgroundJobActor extends BaseActor {

  private ElasticSearchService esUtil = EsClientFactory.getInstance(JsonKey.REST);

  @Override
  public void onReceive(Request request) throws Throwable {
    String operation = request.getOperation();
    switch (operation) {
      case "upsertUserDetailsToES":
        saveUserDataToES(request);
        break;
      case "upsertUserOrgDetailsToES":
        saveUserOrgDetailsToES(request);
        break;
      default:
        onReceiveUnsupportedOperation("UserBackgroundJobActor");
        break;
    }
  }

  private void saveUserOrgDetailsToES(Request request) {
    Map<String, Object> userDetails = request.getRequest();
    Map<String, Object> userOrgMap = new HashMap<>();
    userOrgMap.put(JsonKey.ID, userDetails.get(JsonKey.ID));
    userOrgMap.put(
        JsonKey.ORGANISATIONS,
        UserUtil.getActiveUserOrgDetails(
            (String) userDetails.get(JsonKey.ID), request.getRequestContext()));
    logger.info(request.getRequestContext(), "Updating saveUserOrgDetailsToES");
    upsertDataToElastic(
        ProjectUtil.EsIndex.sunbird.getIndexName(),
        ProjectUtil.EsType.user.getTypeName(),
        (String) userDetails.get(JsonKey.ID),
        userOrgMap,
        request.getRequestContext());
  }

  private void saveUserDataToES(Request request) {
    Map<String, Object> userDetails = request.getRequest();
    logger.info(request.getRequestContext(), "Updating saveUserDataToES");
    userDetails.remove(JsonKey.PASSWORD);
    ObjectMapper mapper = new ObjectMapper();
    User user = mapper.convertValue(userDetails, User.class);
    userDetails = mapper.convertValue(user, Map.class);
    upsertDataToElastic(
        ProjectUtil.EsIndex.sunbird.getIndexName(),
        ProjectUtil.EsType.user.getTypeName(),
        (String) userDetails.get(JsonKey.ID),
        userDetails,
        request.getRequestContext());
  }

  private void upsertDataToElastic(
      String indexName,
      String typeName,
      String id,
      Map<String, Object> userDetails,
      RequestContext context) {

    Future<Boolean> bool = esUtil.upsert(typeName, id, userDetails, context);

    logger.info(
        context,
        "Getting ES save response for type , identifier=="
            + typeName
            + "  "
            + id
            + "  "
            + ElasticSearchHelper.getResponseFromFuture(bool));
  }
}
