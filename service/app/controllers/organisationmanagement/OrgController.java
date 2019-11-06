package controllers.organisationmanagement;

import controllers.BaseController;
import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.models.util.ProjectUtil.EsType;
import org.sunbird.common.request.BaseRequestValidator;
import org.sunbird.common.request.Request;
import org.sunbird.common.request.orgvalidator.OrgRequestValidator;
import play.mvc.Http;
import play.mvc.Result;

import java.util.concurrent.CompletionStage;

public class OrgController extends BaseController {

  public CompletionStage<Result> createOrg(Http.Request httpRequest) {
    return handleRequest(
        ActorOperations.CREATE_ORG.getValue(),
        httpRequest.body().asJson(),
        orgRequest -> {
          new OrgRequestValidator().validateCreateOrgRequest((Request) orgRequest);
          return null;
        },
        getAllRequestHeaders(httpRequest),
            httpRequest);
  }

  public CompletionStage<Result> updateOrg(Http.Request httpRequest) {
    return handleRequest(
        ActorOperations.UPDATE_ORG.getValue(),
        httpRequest.body().asJson(),
        orgRequest -> {
          new OrgRequestValidator().validateUpdateOrgRequest((Request) orgRequest);
          return null;
        },
        getAllRequestHeaders(httpRequest),
            httpRequest);
  }

  public CompletionStage<Result> updateOrgStatus(Http.Request httpRequest) {
    return handleRequest(
        ActorOperations.UPDATE_ORG_STATUS.getValue(),
        httpRequest.body().asJson(),
        orgRequest -> {
          new OrgRequestValidator().validateUpdateOrgStatusRequest((Request) orgRequest);
          return null;
        },
        getAllRequestHeaders(httpRequest),
            httpRequest);
  }

  public CompletionStage<Result> getOrgDetails(Http.Request httpRequest) {
    return handleRequest(
        ActorOperations.GET_ORG_DETAILS.getValue(),
        httpRequest.body().asJson(),
        orgRequest -> {
          new OrgRequestValidator().validateOrgReference((Request) orgRequest);
          return null;
        },
        getAllRequestHeaders(httpRequest),
            httpRequest);
  }

  public CompletionStage<Result> search(Http.Request httpRequest) {
    return handleSearchRequest(
        ActorOperations.COMPOSITE_SEARCH.getValue(),
        httpRequest.body().asJson(),
        orgRequest -> {
          new BaseRequestValidator().validateSearchRequest((Request) orgRequest);
          return null;
        },
        null,
        null,
        getAllRequestHeaders(httpRequest),
        EsType.organisation.getTypeName(),
            httpRequest);
  }
}
