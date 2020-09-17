package org.sunbird.common.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
import javax.ws.rs.core.MediaType;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHeaders;
import org.sunbird.common.models.util.*;
import org.sunbird.common.request.RequestContext;

/**
 * Keycloak utility to create required action links.
 *
 * @author Amit Kumar
 */
public class KeycloakRequiredActionLinkUtil {
  private static LoggerUtil logger = new LoggerUtil(KeycloakRequiredActionLinkUtil.class);
  public static final String VERIFY_EMAIL = "VERIFY_EMAIL";
  public static final String UPDATE_PASSWORD = "UPDATE_PASSWORD";
  private static final String CLIENT_ID = "clientId";
  private static final String REQUIRED_ACTION = "requiredAction";
  private static final String USERNAME = "userName";
  private static final String EXPIRATION_IN_SEC = "expirationInSecs";
  private static final String REDIRECT_URI = "redirectUri";
  private static final String ACCESS_TOKEN = "access_token";
  private static final String SUNBIRD_KEYCLOAK_LINK_EXPIRATION_TIME =
      "sunbird_keycloak_required_action_link_expiration_seconds";
  private static final String SUNBIRD_KEYCLOAK_REQD_ACTION_LINK = "/get-required-action-link";
  private static final String LINK = "link";

  private static ObjectMapper mapper = new ObjectMapper();

  /**
   * Get generated link for specified type and user from Keycloak service.
   *
   * @param userName User name
   * @param requiredAction Type of link to be generated. Supported types are UPDATE_PASSWORD and
   *     VERIFY_EMAIL.
   * @return Generated link from Keycloak service
   */
  public static String getLink(
      String userName, String redirectUri, String requiredAction, RequestContext context) {
    Map<String, String> request = new HashMap<>();

    request.put(CLIENT_ID, ProjectUtil.getConfigValue(JsonKey.SUNBIRD_SSO_CLIENT_ID));
    request.put(USERNAME, userName);
    request.put(REQUIRED_ACTION, requiredAction);

    String expirationInSecs = ProjectUtil.getConfigValue(SUNBIRD_KEYCLOAK_LINK_EXPIRATION_TIME);
    if (StringUtils.isNotBlank(expirationInSecs)) {
      request.put(EXPIRATION_IN_SEC, expirationInSecs);
    }
    request.put(REDIRECT_URI, redirectUri);

    try {
      Thread.sleep(
          Integer.parseInt(ProjectUtil.getConfigValue(JsonKey.SUNBIRD_SYNC_READ_WAIT_TIME)));
      return generateLink(request, context);
    } catch (Exception ex) {
      logger.error(
          context,
          "KeycloakRequiredActionLinkUtil:getLink: Exception occurred with error message = "
              + ex.getMessage(),
          ex);
    }
    return null;
  }

  private static String generateLink(Map<String, String> request, RequestContext context)
      throws Exception {
    Map<String, String> headers = new HashMap<>();

    headers.put(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
    headers.put(JsonKey.AUTHORIZATION, JsonKey.BEARER + getAdminAccessToken(context));

    logger.info(
        context,
        "KeycloakRequiredActionLinkUtil:generateLink: complete URL "
            + ProjectUtil.getConfigValue(JsonKey.SUNBIRD_SSO_URL)
            + "realms/"
            + ProjectUtil.getConfigValue(JsonKey.SUNBIRD_SSO_RELAM)
            + SUNBIRD_KEYCLOAK_REQD_ACTION_LINK);
    logger.info(
        context,
        "KeycloakRequiredActionLinkUtil:generateLink: request body "
            + mapper.writeValueAsString(request));
    String url =
        ProjectUtil.getConfigValue(JsonKey.SUNBIRD_SSO_URL)
            + "realms/"
            + ProjectUtil.getConfigValue(JsonKey.SUNBIRD_SSO_RELAM)
            + SUNBIRD_KEYCLOAK_REQD_ACTION_LINK;
    String response = HttpClientUtil.post(url, mapper.writeValueAsString(request), headers);

    logger.info(context, "KeycloakRequiredActionLinkUtil:generateLink: Response = " + response);

    Map<String, Object> responseMap = new ObjectMapper().readValue(response, Map.class);
    return (String) responseMap.get(LINK);
  }

  public static String getAdminAccessToken(RequestContext context) throws Exception {
    Map<String, String> headers = new HashMap<>();
    headers.put(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED);
    String url =
        ProjectUtil.getConfigValue(JsonKey.SUNBIRD_SSO_URL)
            + "realms/"
            + ProjectUtil.getConfigValue(JsonKey.SUNBIRD_SSO_RELAM)
            + "/protocol/openid-connect/token";

    Map<String, String> fields = new HashMap<>();
    fields.put("client_id", ProjectUtil.getConfigValue(JsonKey.SUNBIRD_SSO_CLIENT_ID));
    fields.put("client_secret", ProjectUtil.getConfigValue(JsonKey.SUNBIRD_SSO_CLIENT_SECRET));
    fields.put("grant_type", "client_credentials");

    String response = HttpClientUtil.postFormData(url, fields, headers);

    logger.info(
        context, "KeycloakRequiredActionLinkUtil:getAdminAccessToken: Response = " + response);
    Map<String, Object> responseMap = new ObjectMapper().readValue(response, Map.class);
    return (String) responseMap.get(ACCESS_TOKEN);
  }
}
