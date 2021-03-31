package util;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.utils.URIBuilder;
import org.sunbird.common.models.util.*;

public class CaptchaHelper {
  private static LoggerUtil logger = new LoggerUtil(CaptchaHelper.class);

  String captchaUrl = null;
  String mobilePrivateKey = null;
  String portalPrivateKey = null;
  ObjectMapper mapper = new ObjectMapper();

  public CaptchaHelper() {
    captchaUrl = "https://www.google.com/recaptcha/api/siteverify";
    mobilePrivateKey = ProjectUtil.getConfigValue(JsonKey.GOOGLE_CAPTCHA_MOBILE_PRIVATE_KEY);
    portalPrivateKey = ProjectUtil.getConfigValue(JsonKey.GOOGLE_CAPTCHA_PRIVATE_KEY);
  }

  public boolean validate(String captcha, String mobileApp) {
    boolean isCaptchaValid = false;
    String url = null;
    Map requestMap = new HashMap<String, String>();
    String secret = StringUtils.isNotEmpty(mobileApp) ? mobilePrivateKey : portalPrivateKey;
    requestMap.put(JsonKey.RESPONSE, captcha);
    requestMap.put("secret", secret);
    Map<String, String> headers = new HashMap<>();
    headers.put("Accept", "application/json");
    headers.put("Content-type", "application/json");
    try {

      url =
          new URIBuilder(captchaUrl)
              .addParameter(JsonKey.RESPONSE, captcha)
              .addParameter("secret", secret)
              .build()
              .toString();
      logger.info("Calling Api: "+url);
      logger.info("Captcha: "+captcha);
      String response = HttpClientUtil.postFormData(url, requestMap, headers);
      Map<String, Object> responseMap = mapper.readValue(response, Map.class);
      isCaptchaValid = (boolean) responseMap.get("success");
      if (!isCaptchaValid) {
        List<String> errorLst = (List<String>) responseMap.get("error-codes");
        logger.info(
            "exception in validating the google captcha: " + Arrays.toString(errorLst.toArray()));
      }
    } catch (Exception e) {
      logger.error("exception in processing the captcha: " + captcha, e);
    }
    return isCaptchaValid;
  }
}
