package controllers.usermanagement;

import controllers.BaseController;
import controllers.usermanagement.validator.ResetPasswordRequestValidator;
import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.request.Request;
import play.mvc.Http;
import play.mvc.Result;

import java.util.concurrent.CompletionStage;

/**
 * 
 * This controller contains method for reset the user password.
 *
 */
public class ResetPasswordController extends BaseController{

	/**
	 * This method will reset the password for given userId in request.
	 * @return Promise
	 */
	public CompletionStage<Result> resetPassword(Http.Request httpRequest) {
	    return handleRequest(
	        ActorOperations.RESET_PASSWORD.getValue(),
	        httpRequest.body().asJson(),
	        (request) -> {
	          new ResetPasswordRequestValidator().validateResetPasswordRequest((Request) request);
	          return null;
	        },
	        getAllRequestHeaders(httpRequest), 
							httpRequest);
	  }
}
