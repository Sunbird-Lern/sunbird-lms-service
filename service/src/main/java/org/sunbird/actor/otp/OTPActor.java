package org.sunbird.actor.otp;

import java.text.MessageFormat;
import java.util.Map;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.actor.core.BaseActor;
import org.sunbird.actor.router.ActorConfig;
import org.sunbird.datasecurity.impl.LogMaskServiceImpl;
import org.sunbird.exception.ProjectCommonException;
import org.sunbird.exception.ResponseCode;
import org.sunbird.keys.JsonKey;
import org.sunbird.operations.ActorOperations;
import org.sunbird.request.Request;
import org.sunbird.request.RequestContext;
import org.sunbird.response.ClientErrorResponse;
import org.sunbird.response.Response;
import org.sunbird.service.otp.OTPService;
import org.sunbird.service.ratelimit.RateLimitService;
import org.sunbird.service.ratelimit.RateLimitServiceImpl;
import org.sunbird.telemetry.dto.TelemetryEnvKey;
import org.sunbird.util.otp.OTPUtil;
import org.sunbird.util.ProjectUtil;
import org.sunbird.util.Util;
import org.sunbird.util.ratelimit.OtpRateLimiter;
import org.sunbird.util.ratelimit.RateLimiter;

@ActorConfig(
  tasks = {"generateOTP", "verifyOTP"},
  asyncTasks = {},
  dispatcher = "notification-dispatcher"
)
public class OTPActor extends BaseActor {

  private OTPService otpService = new OTPService();
  private static final String SUNBIRD_OTP_ALLOWED_ATTEMPT = "sunbird_otp_allowed_attempt";
  private static final String REMAINING_ATTEMPT = "remainingAttempt";
  private static final String MAX_ALLOWED_ATTEMPT = "maxAllowedAttempt";
  private RateLimitService rateLimitService = new RateLimitServiceImpl();
  private LogMaskServiceImpl logMaskService = new LogMaskServiceImpl();

  @Override
  public void onReceive(Request request) throws Throwable {
    Util.initializeContext(request, TelemetryEnvKey.USER);
    if (ActorOperations.GENERATE_OTP.getValue().equals(request.getOperation())) {
      generateOTP(request);
    } else if (ActorOperations.VERIFY_OTP.getValue().equals(request.getOperation())) {
      verifyOTP(request);
    } else {
      onReceiveUnsupportedOperation("OTPActor");
    }
  }

  private void generateOTP(Request request) {
    logger.debug(request.getRequestContext(), "OTPActor:generateOTP method call start.");
    String type = (String) request.getRequest().get(JsonKey.TYPE);
    String key = (String) request.getRequest().get(JsonKey.KEY);

    String userId = (String) request.getRequest().get(JsonKey.USER_ID);
    if (StringUtils.isNotBlank(userId)) {
      key = otpService.getEmailPhoneByUserId(userId, type, request.getRequestContext());
      type = getType(type);
      logger.info(
          request.getRequestContext(),
          "OTPActor:generateOTP:getEmailPhoneByUserId: called for userId = "
              + userId
              + " ,key = "
              + maskId(key, type));
    }

    rateLimitService.throttleByKey(
        key,
        new RateLimiter[] {OtpRateLimiter.HOUR, OtpRateLimiter.DAY},
        request.getRequestContext());

    String otp;
    Map<String, Object> details = otpService.getOTPDetails(type, key, request.getRequestContext());

    if (MapUtils.isEmpty(details)) {
      otp = OTPUtil.generateOTP(request.getRequestContext());
      logger.info(
          request.getRequestContext(),
          "OTPActor:generateOTP: new otp generated for Key = "
              + maskId(key, type)
              + " & OTP = "
              + maskOTP(otp));
      otpService.insertOTPDetails(type, key, otp, request.getRequestContext());
    } else {
      otp = (String) details.get(JsonKey.OTP);
      logger.info(
          request.getRequestContext(),
          "OTPActor:generateOTP: Re-issuing otp for Key = "
              + maskId(key, type)
              + " & OTP = "
              + maskOTP(otp));
    }
    logger.info(
        request.getRequestContext(),
        "OTPActor:sendOTP : Calling SendOTPActor for Key = " + maskId(key, type));
    sendOTP(request, otp, key, request.getRequestContext());

    Response response = new Response();
    response.put(JsonKey.RESPONSE, JsonKey.SUCCESS);
    sender().tell(response, self());
  }

  private void verifyOTP(Request request) {
    String type = (String) request.getRequest().get(JsonKey.TYPE);
    String key = (String) request.getRequest().get(JsonKey.KEY);
    String otpInRequest = (String) request.getRequest().get(JsonKey.OTP);

    String userId = (String) request.getRequest().get(JsonKey.USER_ID);
    if (StringUtils.isNotBlank(userId)) {
      key = otpService.getEmailPhoneByUserId(userId, type, request.getRequestContext());
      type = getType(type);
      logger.info(
        request.getRequestContext(),
        "OTPActor:verifyOTP:getEmailPhoneByUserId: called for userId = "
          + userId
          + " ,key = "
          + maskId(key, type));
    }
    Map<String, Object> otpDetails =
        otpService.getOTPDetails(type, key, request.getRequestContext());

    if (MapUtils.isEmpty(otpDetails)) {
      logger.info(
          request.getRequestContext(),
          "OTP_VALIDATION_FAILED:OTPActor:verifyOTP: Details not found for Key = "
              + maskId(key, type)
              + " type = "
              + type);
      ProjectCommonException.throwClientErrorException(ResponseCode.errorInvalidOTP);
    }
    String otpInDB = (String) otpDetails.get(JsonKey.OTP);
    if (StringUtils.isBlank(otpInDB) || StringUtils.isBlank(otpInRequest)) {
      logger.info(
          request.getRequestContext(),
          "OTP_VALIDATION_FAILED : OTPActor:verifyOTP: Mismatch for Key = "
              + maskId(key, type)
              + " otpInRequest = "
              + maskOTP(otpInRequest)
              + " otpInDB = "
              + maskOTP(otpInDB));
      ProjectCommonException.throwClientErrorException(ResponseCode.errorInvalidOTP);
    }

    if (otpInRequest.equals(otpInDB)) {
      logger.info(
          request.getRequestContext(),
          "OTP_VALIDATION_SUCCESS:OTPActor:verifyOTP: Verified successfully Key = "
              + maskId(key, type));
      otpService.deleteOtp(type, key, request.getRequestContext());
      Response response = new Response();
      response.put(JsonKey.RESPONSE, JsonKey.SUCCESS);
      sender().tell(response, self());
    } else {
      logger.info(
          request.getRequestContext(),
          "OTP_VALIDATION_FAILED: OTPActor:verifyOTP: Incorrect OTP Key = "
              + maskId(key, type)
              + " otpInRequest = "
              + maskOTP(otpInRequest)
              + " otpInDB = "
              + maskOTP(otpInDB));
      handleMismatchOtp(type, key, otpDetails, request.getRequestContext());
    }
  }

  private void handleMismatchOtp(
      String type, String key, Map<String, Object> otpDetails, RequestContext context) {
    int remainingCount = getRemainingAttemptedCount(otpDetails);
    logger.info(
        context,
        "OTPActor:handleMismatchOtp: Key = "
            + maskId(key, type)
            + ",remaining attempt is "
            + remainingCount);
    int attemptedCount = (int) otpDetails.get(JsonKey.ATTEMPTED_COUNT);
    if (remainingCount <= 0) {
      otpService.deleteOtp(type, key, context);
    } else {
      otpDetails.put(JsonKey.ATTEMPTED_COUNT, attemptedCount + 1);
      otpService.updateAttemptCount(otpDetails, context);
    }
    ProjectCommonException ex =
        new ProjectCommonException(
            ResponseCode.otpVerificationFailed.getErrorCode(),
            MessageFormat.format(
                ResponseCode.otpVerificationFailed.getErrorMessage(), remainingCount),
            ResponseCode.CLIENT_ERROR.getResponseCode());

    ClientErrorResponse response = new ClientErrorResponse();
    response.setException(ex);
    response
        .getResult()
        .put(
            MAX_ALLOWED_ATTEMPT,
            Integer.parseInt(ProjectUtil.getConfigValue(SUNBIRD_OTP_ALLOWED_ATTEMPT)));
    response.getResult().put(REMAINING_ATTEMPT, remainingCount);
    sender().tell(response, self());
  }

  private int getRemainingAttemptedCount(Map<String, Object> otpDetails) {
    int allowedAttempt = Integer.parseInt(ProjectUtil.getConfigValue(SUNBIRD_OTP_ALLOWED_ATTEMPT));
    int attemptedCount = (int) otpDetails.get(JsonKey.ATTEMPTED_COUNT);
    return (allowedAttempt - (attemptedCount + 1));
  }

  private void sendOTP(Request request, String otp, String key, RequestContext context) {
    Request sendOtpRequest = new Request();
    sendOtpRequest.setRequestContext(context);
    sendOtpRequest.getRequest().putAll(request.getRequest());
    sendOtpRequest.getRequest().put(JsonKey.KEY, key);
    sendOtpRequest.getRequest().put(JsonKey.OTP, otp);
    sendOtpRequest.setOperation(ActorOperations.SEND_OTP.getValue());
    tellToAnother(sendOtpRequest);
  }

  private String getType(String type) {
    switch (type) {
      case JsonKey.PREV_USED_EMAIL:
      case JsonKey.RECOVERY_EMAIL:
      case JsonKey.EMAIL:
        return JsonKey.EMAIL;
      case JsonKey.PREV_USED_PHONE:
      case JsonKey.RECOVERY_PHONE:
      case JsonKey.PHONE:
        return JsonKey.PHONE;
      default:
        return null;
    }
  }

  private String maskOTP(String otp) {
    return logMaskService.maskOTP(otp);
  }

  private String maskId(String id, String type) {
    if (JsonKey.EMAIL.equalsIgnoreCase(type)) {
      return logMaskService.maskEmail(id);
    } else if (JsonKey.PHONE.equalsIgnoreCase(type)) {
      return logMaskService.maskPhone(id);
    }
    return "";
  }
}
