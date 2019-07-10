package controllers.otp.validator;

import java.util.Map;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.models.util.StringFormatter;
import org.sunbird.common.request.BaseRequestValidator;
import org.sunbird.common.request.Request;
import org.sunbird.common.responsecode.ResponseCode;

public class OtpRequestValidator extends BaseRequestValidator {

  public void validateGenerateOtpRequest(Request otpRequest) {
    commonValidation(otpRequest, false);
  }

  public void validateVerifyOtpRequest(Request otpRequest) {
    commonValidation(otpRequest, true);
  }

  private void commonValidation(Request otpRequest, boolean isOtpMandatory) {
    validateParam(
        (String) otpRequest.getRequest().get(JsonKey.KEY),
        ResponseCode.mandatoryParamsMissing,
        JsonKey.KEY);
    validateParam(
        (String) otpRequest.getRequest().get(JsonKey.TYPE),
        ResponseCode.mandatoryParamsMissing,
        JsonKey.TYPE);
    if (isOtpMandatory) {
      validateParam(
          (String) otpRequest.getRequest().get(JsonKey.OTP),
          ResponseCode.mandatoryParamsMissing,
          JsonKey.OTP);
    }
    validateTypeAndKey(otpRequest);
  }

  private void validateTypeAndKey(Request otpRequest) {
    Map<String, Object> requestMap = otpRequest.getRequest();

    String type = (String) requestMap.get(JsonKey.TYPE);
    String key = (String) requestMap.get(JsonKey.KEY);

    if (JsonKey.EMAIL.equalsIgnoreCase(type)) {
      validateEmail(key);
    } else if (JsonKey.PHONE.equalsIgnoreCase(type)) {
      validatePhone(key);
    } else {
      ProjectCommonException.throwClientErrorException(
          ResponseCode.invalidValue,
          ProjectUtil.formatMessage(
              ResponseCode.invalidValue.getErrorMessage(),
              JsonKey.TYPE,
              type,
              String.join(StringFormatter.COMMA, JsonKey.EMAIL, JsonKey.PHONE)));
    }
  }
}
