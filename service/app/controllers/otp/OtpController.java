package controllers.otp;

import controllers.BaseController;
import controllers.otp.validator.OtpRequestValidator;
import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.request.Request;
import play.mvc.Http;
import play.mvc.Result;

import java.util.concurrent.CompletionStage;

public class OtpController extends BaseController {

  public CompletionStage<Result> generateOTP(Http.Request httpRequest) {
    return handleRequest(
        ActorOperations.GENERATE_OTP.getValue(),
        httpRequest.body().asJson(),
        (request) -> {
          new OtpRequestValidator().validateGenerateOtpRequest((Request) request);
          return null;
        },
        getAllRequestHeaders(httpRequest),
            httpRequest);
  }

  public CompletionStage<Result> verifyOTP(Http.Request httpRequest) {
    return handleRequest(
        ActorOperations.VERIFY_OTP.getValue(),
        httpRequest.body().asJson(),
        (request) -> {
          new OtpRequestValidator().validateVerifyOtpRequest((Request) request);
          return null;
        },
        getAllRequestHeaders(httpRequest),
            httpRequest);
  }
}
