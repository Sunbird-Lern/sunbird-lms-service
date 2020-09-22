package controllers.usermanagement;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import controllers.BaseApplicationTest;
import controllers.DummyActor;
import modules.OnRequestHandler;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.sunbird.common.models.util.HttpClientUtil;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.request.HeaderParam;
import play.libs.Json;
import play.mvc.Http;
import play.mvc.Result;
import play.test.Helpers;

import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertTrue;

@PrepareForTest({
        OnRequestHandler.class,
        ProjectUtil.class,
        HttpClientUtil.class
})
public class UserConsentControllerTest extends BaseApplicationTest {

    public static Map<String, List<String>> headerMap;
    @Before
    public void before() {
        setup(DummyActor.class);
        headerMap = new HashMap<>();
        headerMap.put(HeaderParam.X_Consumer_ID.getName(), Arrays.asList("Some consumer ID"));
        headerMap.put(HeaderParam.X_Device_ID.getName(), Arrays.asList("Some device ID"));
        headerMap.put(
                HeaderParam.X_Authenticated_Userid.getName(), Arrays.asList("Some authenticated user ID"));
        headerMap.put(JsonKey.MESSAGE_ID, Arrays.asList("Some message ID"));
        headerMap.put(HeaderParam.X_APP_ID.getName(), Arrays.asList("Some app Id"));
    }

    @Test
    public void updateUserConsentSuccess() {
        Result result =
                performTest(
                        "/v1/user/consent/update",
                        "POST",
                        (Map) updateUserConsentRequest());
        assertTrue(getResponseStatus(result) == Response.Status.OK.getStatusCode());
    }

    @Test
    public void getUserConsentSuccess() {
        Result result =
                performTest(
                        "/v1/user/consent/read",
                        "POST",
                        (Map) getUserConsentRequest());
        assertTrue(getResponseStatus(result) == Response.Status.OK.getStatusCode());
    }


    public static Map<String, Object> getUserConsentRequest(){
        Map<String, Object> filters = new HashMap<String, Object>();
        filters.put(JsonKey.USER_ID, "test-user");
        filters.put(JsonKey.CONSENT_CONSUMERID, "test-organisation");
        filters.put(JsonKey.CONSENT_OBJECTID, "test-collection");

        Map<String, Object> consent = new HashMap<String, Object>();
        consent.put("filters", filters);

        Map<String, Object> innerMap = new HashMap<String, Object>();
        innerMap.put("consent",consent);

        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put(JsonKey.REQUEST, innerMap);

        return requestMap;
    }

    public static Map<String, Object> updateUserConsentRequest(){
        Map<String, Object> consent = new HashMap<String, Object>();
        consent.put(JsonKey.USER_ID, "test-user");
        consent.put(JsonKey.CONSENT_CONSUMERID, "test-organisation");
        consent.put(JsonKey.CONSENT_OBJECTID, "test-collection");
        consent.put(JsonKey.CONSENT_OBJECTTYPE, "Collection");
        consent.put(JsonKey.STATUS, "ACTIVE");


        Map<String, Object> innerMap = new HashMap<String, Object>();
        innerMap.put("consent",consent);

        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put(JsonKey.REQUEST, innerMap);

        return requestMap;
    }

    public Result performTest(String url, String method, Map map) {
        String data = mapToJson(map);
        Http.RequestBuilder req;
        if (StringUtils.isNotBlank(data)) {
            JsonNode json = Json.parse(data);
            req = new Http.RequestBuilder().bodyJson(json).uri(url).method(method);
        } else {
            req = new Http.RequestBuilder().uri(url).method(method);
        }
        // req.headers(new Http.Headers(headerMap));
        Result result = Helpers.route(application, req);
        return result;
    }


    public String mapToJson(Map map) {
        ObjectMapper mapperObj = new ObjectMapper();
        String jsonResp = "";

        if (map != null) {
            try {
                jsonResp = mapperObj.writeValueAsString(map);
            } catch (IOException e) {
                ProjectLogger.log(e.getMessage(), e);
            }
        }
        return jsonResp;
    }
}
