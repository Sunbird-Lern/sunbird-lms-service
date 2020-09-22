package controllers.usermanagement;

import controllers.BaseController;
import controllers.usermanagement.validator.ResetPasswordRequestValidator;
import controllers.usermanagement.validator.UserConsentRequestValidator;
import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.request.Request;
import play.mvc.Http;
import play.mvc.Result;

import java.util.concurrent.CompletionStage;

public class UserConsentController extends BaseController {

    public CompletionStage<Result> updateUserConsent(Http.Request httpRequest) {
        return handleRequest(
                ActorOperations.UPDATE_USER_CONSENT.getValue(),
                httpRequest.body().asJson(),
                req -> {
                    Request request = (Request) req;
                    new UserConsentRequestValidator().validateUpdateConsentRequest((Request) request);
                    return null;
                },
                null,
                null,
                true,
                httpRequest);
    }

    public CompletionStage<Result> getUserConsent(Http.Request httpRequest) {
        return handleRequest(
                ActorOperations.GET_USER_CONSENT.getValue(),
                httpRequest.body().asJson(),
                req -> {
                    Request request = (Request) req;
                    new UserConsentRequestValidator().validateReadConsentRequest((Request) request);
                    return null;
                },
                null,
                null,
                true,
                httpRequest);
    }
}
