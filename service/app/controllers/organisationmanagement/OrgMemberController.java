package controllers.organisationmanagement;

import controllers.BaseController;
import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.request.Request;
import org.sunbird.common.request.orgvalidator.OrgMemberRequestValidator;
import play.libs.F.Promise;
import play.mvc.Result;

public class OrgMemberController extends BaseController {

  public Promise<Result> addMemberToOrganisation() {
    return handleRequest(
        ActorOperations.ADD_MEMBER_ORGANISATION.getValue(),
        request().body().asJson(),
        orgRequest -> {
          new OrgMemberRequestValidator().validateAddMemberRequest((Request) orgRequest);
          return null;
        },
        getAllRequestHeaders(request()));
  }

  public Promise<Result> removeMemberFromOrganisation() {
    return handleRequest(
        ActorOperations.REMOVE_MEMBER_ORGANISATION.getValue(),
        request().body().asJson(),
        orgRequest -> {
          new OrgMemberRequestValidator().validateCommon((Request) orgRequest);
          return null;
        },
        getAllRequestHeaders(request()));
  }
}
