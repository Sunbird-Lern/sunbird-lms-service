package controllers.tenantmigration;

import controllers.BaseController;
import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.request.Request;
import org.sunbird.common.request.UserTenantMigrationRequestValidator;
import play.libs.F.Promise;
import play.mvc.Result;

/** @author Amit Kumar This controller will handle all the request related for user migration. */
public class TenantMigrationController extends BaseController {

  /**
   * Method to migrate user from one tenant to another.
   *
   * @return Result
   */
  public Promise<Result> userTenantMigrate() {
    return handleRequest(
        ActorOperations.USER_TENANT_MIGRATE.getValue(),
        request().body().asJson(),
        req -> {
          Request request = (Request) req;
          new UserTenantMigrationRequestValidator().validateUserTenantMigrateRequest(request);
          return null;
        },
        null,
        null,
        true);
  }
}
