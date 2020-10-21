package org.sunbird.learner.organisation.external.identity.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.request.RequestContext;
import org.sunbird.helper.ServiceFactory;

public class OrgExternalService {

  private final String KEYSPACE_NAME = "sunbird";
  private final String ORG_EXTERNAL_IDENTITY = "org_external_identity";

  public String getOrgIdFromOrgExternalIdAndProvider(
      String externalId, String provider, RequestContext context) {
    Map<String, Object> dbRequestMap = new LinkedHashMap<>(3);
    dbRequestMap.put(JsonKey.PROVIDER, provider.toLowerCase());
    dbRequestMap.put(JsonKey.EXTERNAL_ID, externalId.toLowerCase());
    Response response =
        getCassandraOperation()
            .getRecordsByCompositeKey(KEYSPACE_NAME, ORG_EXTERNAL_IDENTITY, dbRequestMap, context);
    List<Map<String, Object>> orgList =
        (List<Map<String, Object>>) response.getResult().get(JsonKey.RESPONSE);
    if (CollectionUtils.isNotEmpty(orgList)) {
      Map<String, Object> orgExternalMap = orgList.get(0);
      if (MapUtils.isNotEmpty(orgExternalMap)) {
        return (String) orgExternalMap.get(JsonKey.ORG_ID);
      }
    }
    return null;
  }

  private CassandraOperation getCassandraOperation() {
    return ServiceFactory.getInstance();
  }
}
