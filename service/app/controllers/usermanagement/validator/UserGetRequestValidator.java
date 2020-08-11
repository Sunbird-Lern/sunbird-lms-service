package controllers.usermanagement.validator;

import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.models.util.StringFormatter;
import org.sunbird.common.request.BaseRequestValidator;
import org.sunbird.common.request.Request;
import org.sunbird.common.responsecode.ResponseCode;
import play.mvc.Http;
import util.CaptchaHelper;

public class UserGetRequestValidator extends BaseRequestValidator {

  public void validateGetUserByKeyRequest(Request request) {
    String key = (String) request.getRequest().get(JsonKey.KEY);

    validateParam(key, ResponseCode.mandatoryParamsMissing, JsonKey.KEY);

    validateParam(
        (String) request.getRequest().get(JsonKey.VALUE),
        ResponseCode.mandatoryParamsMissing,
        JsonKey.VALUE);

    if (!(key.equalsIgnoreCase(JsonKey.PHONE)
        || key.equalsIgnoreCase(JsonKey.EMAIL)
        || key.equalsIgnoreCase(JsonKey.LOGIN_ID)
        || key.equalsIgnoreCase(JsonKey.USERNAME))) {

      ProjectCommonException.throwClientErrorException(
          ResponseCode.invalidValue,
          ProjectUtil.formatMessage(
              ResponseCode.invalidValue.getErrorMessage(),
              JsonKey.KEY,
              key,
              String.join(
                  StringFormatter.COMMA,
                  JsonKey.EMAIL,
                  JsonKey.PHONE,
                  JsonKey.LOGIN_ID,
                  JsonKey.USERNAME)));
    }

    if (JsonKey.PHONE.equals(request.get(JsonKey.KEY))) {
      validatePhone((String) request.get(JsonKey.VALUE));
    }
    if (JsonKey.EMAIL.equals(request.get(JsonKey.KEY))) {
      validateEmail((String) request.get(JsonKey.VALUE));
    }
  }

  public void validateGetUserByKeyRequestaWithCaptcha(Request request, Http.Request httpRequest) {
    String captcha = httpRequest.getQueryString(JsonKey.CAPTCHA_RESPONSE);
    String mobileApp = httpRequest.getQueryString(JsonKey.MOBILE_APP);
    if (Boolean.parseBoolean(ProjectUtil.getConfigValue(JsonKey.ENABLE_CAPTCHA))
        && !new CaptchaHelper().validate(captcha, mobileApp)) {
      throw new ProjectCommonException(
          ResponseCode.invalidCaptcha.getErrorCode(),
          ResponseCode.invalidCaptcha.getErrorMessage(),
          ResponseCode.IM_A_TEAPOT.getResponseCode());
    }
    validateGetUserByKeyRequest(request);
  }
}
