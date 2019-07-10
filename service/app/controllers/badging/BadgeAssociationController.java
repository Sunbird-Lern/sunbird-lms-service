package controllers.badging;

import controllers.BaseController;
import controllers.badging.validator.BadgeAssociationValidator;
import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.models.util.BadgingActorOperations;
import org.sunbird.common.models.util.ProjectUtil.EsType;
import org.sunbird.common.request.BaseRequestValidator;
import org.sunbird.common.request.Request;
import play.libs.F.Promise;
import play.mvc.Result;

public class BadgeAssociationController extends BaseController {

  public Promise<Result> createAssociation() {
    return handleRequest(
        BadgingActorOperations.CREATE_BADGE_ASSOCIATION.getValue(),
        request().body().asJson(),
        (request) -> {
          new BadgeAssociationValidator().validateCreateBadgeAssociationRequest((Request) request);
          return null;
        },
        null,
        null,
        true);
  }

  public Promise<Result> removeAssociation() {
    return handleRequest(
        BadgingActorOperations.REMOVE_BADGE_ASSOCIATION.getValue(),
        request().body().asJson(),
        (request) -> {
          new BadgeAssociationValidator().validateRemoveBadgeAssociationRequest((Request) request);
          return null;
        },
        null,
        null,
        true);
  }

  public Promise<Result> searchAssociation() {
    return handleSearchRequest(
        ActorOperations.COMPOSITE_SEARCH.getValue(),
        request().body().asJson(),
        request -> {
          new BaseRequestValidator().validateSearchRequest((Request) request);
          return null;
        },
        null,
        null,
        getAllRequestHeaders(request()),
        EsType.badgeassociations.getTypeName());
  }
}
