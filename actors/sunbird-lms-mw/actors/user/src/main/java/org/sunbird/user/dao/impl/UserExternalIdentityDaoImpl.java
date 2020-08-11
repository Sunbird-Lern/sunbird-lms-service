package org.sunbird.user.dao.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.collections.CollectionUtils;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.datasecurity.EncryptionService;
import org.sunbird.common.responsecode.ResponseCode;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.learner.util.Util;
import org.sunbird.user.dao.UserExternalIdentityDao;

public class UserExternalIdentityDaoImpl implements UserExternalIdentityDao {

  private CassandraOperation cassandraOperation = ServiceFactory.getInstance();
  private EncryptionService encryptionService =
      org.sunbird.common.models.util.datasecurity.impl.ServiceFactory.getEncryptionServiceInstance(
          null);

  /*  @Override
  public String getUserId(Request reqObj) {

    if (StringUtils.isBlank(userId)) {
      String extId = (String) reqObj.getRequest().get(JsonKey.EXTERNAL_ID);
      String provider = (String) reqObj.getRequest().get(JsonKey.EXTERNAL_ID_PROVIDER);
      String idType = (String) reqObj.getRequest().get(JsonKey.EXTERNAL_ID_TYPE);

      userId = getUserIdByExternalId(extId, provider, idType);
    }

    return userId;
  }*/

  @Override
  public String getUserIdByExternalId(String extId, String provider, String idType) {
    Util.DbInfo usrDbInfo = Util.dbInfoMap.get(JsonKey.USER_DB);
    Map<String, Object> externalIdReq = new HashMap<>();
    externalIdReq.put(JsonKey.PROVIDER, provider.toLowerCase());
    externalIdReq.put(JsonKey.ID_TYPE, idType.toLowerCase());
    externalIdReq.put(JsonKey.EXTERNAL_ID, extId.toLowerCase());
    Response response =
        cassandraOperation.getRecordsByProperties(
            usrDbInfo.getKeySpace(), JsonKey.USR_EXT_IDNT_TABLE, externalIdReq);

    List<Map<String, Object>> userRecordList =
        (List<Map<String, Object>>) response.get(JsonKey.RESPONSE);
    if (CollectionUtils.isNotEmpty(userRecordList)) {
      return (String) userRecordList.get(0).get(JsonKey.USER_ID);
    }

    return null;
  }

  @Override
  public List<Map<String, String>> getUserExternalIds(String userId) {
    List<Map<String, String>> dbResExternalIds = new ArrayList<>();
    Response response =
        cassandraOperation.getRecordsByIndexedProperty(
            JsonKey.SUNBIRD, JsonKey.USR_EXT_IDNT_TABLE, JsonKey.USER_ID, userId);
    if (null != response && null != response.getResult()) {
      dbResExternalIds = (List<Map<String, String>>) response.getResult().get(JsonKey.RESPONSE);
    }
    return dbResExternalIds;
  }

  @Override
  public List<Map<String, Object>> getUserSelfDeclaredDetails(String userId) {
    List<Map<String, Object>> dbResExternalIds = new ArrayList<>();
    Response response =
        cassandraOperation.getRecordsByIndexedProperty(
            JsonKey.SUNBIRD, JsonKey.USER_DECLARATION_DB, JsonKey.USER_ID, userId);
    if (null != response && null != response.getResult()) {
      dbResExternalIds = (List<Map<String, Object>>) response.getResult().get(JsonKey.RESPONSE);
    }
    return dbResExternalIds;
  }

  private String getEncryptedData(String value) {
    try {
      return encryptionService.encryptData(value);
    } catch (Exception e) {
      throw new ProjectCommonException(
          ResponseCode.userDataEncryptionError.getErrorCode(),
          ResponseCode.userDataEncryptionError.getErrorMessage(),
          ResponseCode.SERVER_ERROR.getResponseCode());
    }
  }
}
