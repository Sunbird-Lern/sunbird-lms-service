package controllers.usermanagement;

import controllers.BaseController;
import java.util.concurrent.CompletionStage;
import org.sunbird.operations.ActorOperations;
import org.sunbird.request.Request;
import org.sunbird.validator.UserFreeUpRequestValidator;
import play.mvc.Http;
import play.mvc.Result;

/**
 * this action method is responsible to free Up the user account attributes.
 *
 * @author anmolgupta
 */
public class IdentifierFreeUpController extends BaseController {

  /**
   * this action method will be used to free Up user Identifier from user DB
   *
   * @return
   */
  public CompletionStage<Result> freeUpIdentifier(Http.Request httpRequest) {
    return handleRequest(
        ActorOperations.FREEUP_USER_IDENTITY.getValue(),
        httpRequest.body().asJson(),
        req -> {
          Request request = (Request) req;
          UserFreeUpRequestValidator.getInstance(request).validate();
          return null;
        },
        null,
        null,
        true,
        httpRequest);
  }
}
