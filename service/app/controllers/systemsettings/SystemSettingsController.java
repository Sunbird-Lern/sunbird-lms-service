package controllers.systemsettings;

import controllers.BaseController;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import org.sunbird.keys.JsonKey;
import org.sunbird.operations.ActorOperations;
import org.sunbird.request.Request;
import org.sunbird.validator.systemsettings.SystemSettingsRequestValidator;
import play.mvc.Http;
import play.mvc.Result;

public class SystemSettingsController extends BaseController {

  private static final SystemSettingsRequestValidator systemSettingsRequestValidator =
      new SystemSettingsRequestValidator();

  @SuppressWarnings("unchecked")
  public CompletionStage<Result> getSystemSetting(String field, Http.Request httpRequest) {
    try {
      return handleRequest(
          ActorOperations.GET_SYSTEM_SETTING.getValue(), field, JsonKey.FIELD, httpRequest);
    } catch (Exception e) {
      return CompletableFuture.completedFuture(createCommonExceptionResponse(e, httpRequest));
    }
  }

  @SuppressWarnings("unchecked")
  public CompletionStage<Result> setSystemSetting(Http.Request httpRequest) {
    try {
      return handleRequest(
          ActorOperations.SET_SYSTEM_SETTING.getValue(),
          httpRequest.body().asJson(),
          (request) -> {
            systemSettingsRequestValidator.validateSetSystemSetting((Request) request);
            return null;
          },
          httpRequest);
    } catch (Exception e) {
      return CompletableFuture.completedFuture(createCommonExceptionResponse(e, httpRequest));
    }
  }

  @SuppressWarnings("unchecked")
  public CompletionStage<Result> getAllSystemSettings(Http.Request httpRequest) {
    try {
      return handleRequest(ActorOperations.GET_ALL_SYSTEM_SETTINGS.getValue(), httpRequest);
    } catch (Exception e) {
      return CompletableFuture.completedFuture(createCommonExceptionResponse(e, httpRequest));
    }
  }
}
