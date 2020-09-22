package org.sunbird.user.dao.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.collections.CollectionUtils;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.request.RequestContext;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.learner.util.Util;
import org.sunbird.user.dao.UserConsentDao;

import java.util.List;
import java.util.Map;

public class UserConsentDaoImpl implements UserConsentDao {
    private static final String TABLE_NAME = "user_consent";
    private CassandraOperation cassandraOperation = ServiceFactory.getInstance();
    private ObjectMapper mapper = new ObjectMapper();

    private static UserConsentDao consentDao = null;

    public static UserConsentDao getInstance() {
        if (consentDao == null) {
            consentDao = new UserConsentDaoImpl();
        }
        return consentDao;
    }

    @Override
    public Response updateConsent(Map<String, Object> consent, RequestContext context){
        return cassandraOperation.upsertRecord(Util.KEY_SPACE_NAME, TABLE_NAME, consent, context);
    }

    @Override
    public List<Map<String, Object>> getConsent(Map<String, Object> consentReq, RequestContext context) {
        Map<String, Object> consentMap = null;
        Response response =
                cassandraOperation.getRecordsByProperties(Util.KEY_SPACE_NAME, TABLE_NAME, consentReq, context);
        List<Map<String, Object>> responseList =
                (List<Map<String, Object>>) response.get(JsonKey.RESPONSE);
        return responseList;
    }
}
