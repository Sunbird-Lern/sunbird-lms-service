/** */
package util;

import controllers.BaseController;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.actor.service.SunbirdMWService;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.BadgingJsonKey;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.LoggerEnum;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.models.util.ProjectUtil.Environment;
import org.sunbird.common.request.ExecutionContext;
import org.sunbird.common.request.HeaderParam;
import org.sunbird.common.responsecode.ResponseCode;
import org.sunbird.learner.util.SchedulerManager;
import org.sunbird.learner.util.Util;
import org.sunbird.telemetry.util.TelemetryUtil;
import play.Application;
import play.GlobalSettings;
import play.libs.F.Promise;
import play.libs.Json;
import play.mvc.Action;
import play.mvc.Http;
import play.mvc.Http.Context;
import play.mvc.Http.Request;
import play.mvc.Result;
import play.mvc.Results;

/**
 * This class will work as a filter.
 *
 * @author Manzarul
 */
public class Global extends GlobalSettings {

  public static ProjectUtil.Environment env;

  public static Map<String, Map<String, Object>> requestInfo = new HashMap<>();

  public static String ssoPublicKey = "";
  private static final String version = "v1";
  private static final List<String> USER_UNAUTH_STATES =
      Arrays.asList(JsonKey.UNAUTHORIZED, JsonKey.ANONYMOUS);

  private class ActionWrapper extends Action.Simple {
    public ActionWrapper(Action<?> action) {
      this.delegate = action;
    }

    @Override
    public Promise<Result> call(Http.Context ctx) throws java.lang.Throwable {
      ctx.request().headers();
      Promise<Result> result = null;
      ctx.response().setHeader("Access-Control-Allow-Origin", "*");
      // Unauthorized, Anonymous, UserID
      String message = RequestInterceptor.verifyRequestData(ctx);
      // call method to set all the required params for the telemetry event(log)...
      intializeRequestInfo(ctx, message);
      if (!USER_UNAUTH_STATES.contains(message)) {
        ctx.flash().put(JsonKey.USER_ID, message);
        ctx.flash().put(JsonKey.IS_AUTH_REQ, "false");
        for (String uri : RequestInterceptor.restrictedUriList) {
          if (ctx.request().path().contains(uri)) {
            ctx.flash().put(JsonKey.IS_AUTH_REQ, "true");
            break;
          }
        }
        result = delegate.call(ctx);
      } else if (JsonKey.UNAUTHORIZED.equals(message)) {
        result =
            onDataValidationError(
                ctx.request(), message, ResponseCode.UNAUTHORIZED.getResponseCode());
      } else {
        result = delegate.call(ctx);
      }
      return result;
    }
  }

  /**
   * This method will be called on application start up. it will be called only time in it's life
   * cycle.
   *
   * @param app Application
   */
  @Override
  public void onStart(Application app) {
    setEnvironment();
    ssoPublicKey = System.getenv(JsonKey.SSO_PUBLIC_KEY);
    ProjectLogger.log("Server started.. with environment: " + env.name(), LoggerEnum.INFO.name());
    SunbirdMWService.init();
    Util.checkCassandraDbConnections(JsonKey.SUNBIRD);
    Util.checkCassandraDbConnections(JsonKey.SUNBIRD_PLUGIN);
    SchedulerManager.schedule();
    // scheduler should start after few minutes so internally it is sleeping for 4 minute , so
    // it is in separate thread.
    new Thread(
            new Runnable() {
              @Override
              public void run() {
                org.sunbird.common.quartz.scheduler.SchedulerManager.getInstance();
              }
            })
        .start();
  }

  /**
   * This method will be called on each request.
   *
   * @param request Request
   * @param actionMethod Method
   * @return Action
   */
  @Override
  @SuppressWarnings("rawtypes")
  public Action onRequest(Request request, Method actionMethod) {

    String messageId = request.getHeader(JsonKey.MESSAGE_ID);
    if (StringUtils.isBlank(messageId)) {
      UUID uuid = UUID.randomUUID();
      messageId = uuid.toString();
    }
    ExecutionContext.setRequestId(messageId);
    return new ActionWrapper(super.onRequest(request, actionMethod));
  }

  private void intializeRequestInfo(Context ctx, String userId) {
    Request request = ctx.request();
    String actionMethod = ctx.request().method();
    String messageId = ExecutionContext.getRequestId(); // request.getHeader(JsonKey.MESSAGE_ID);
    String url = request.uri();
    String methodName = actionMethod;
    long startTime = System.currentTimeMillis();

    ExecutionContext context = ExecutionContext.getCurrent();
    Map<String, Object> reqContext = new HashMap<>();
    // set env and channel to the
    String channel = request.getHeader(JsonKey.CHANNEL_ID);
    if (StringUtils.isBlank(channel)) {
      channel = JsonKey.DEFAULT_ROOT_ORG_ID;
    }
    reqContext.put(JsonKey.CHANNEL, channel);
    ctx.flash().put(JsonKey.CHANNEL, channel);
    reqContext.put(JsonKey.ENV, getEnv(request));
    reqContext.put(JsonKey.REQUEST_ID, ExecutionContext.getRequestId());
    String appId = request.getHeader(HeaderParam.X_APP_ID.getName());
    // check if in request header X-app-id is coming then that need to
    // be pass in search telemetry.
    if (StringUtils.isNotBlank(appId)) {
      ctx.flash().put(JsonKey.APP_ID, appId);
    }
    if (!USER_UNAUTH_STATES.contains(userId)) {
      reqContext.put(JsonKey.ACTOR_ID, userId);
      reqContext.put(JsonKey.ACTOR_TYPE, JsonKey.USER);
      ctx.flash().put(JsonKey.ACTOR_ID, userId);
      ctx.flash().put(JsonKey.ACTOR_TYPE, JsonKey.USER);
    } else {
      String consumerId = request.getHeader(HeaderParam.X_Consumer_ID.getName());
      if (StringUtils.isBlank(consumerId)) {
        consumerId = JsonKey.DEFAULT_CONSUMER_ID;
      }
      reqContext.put(JsonKey.ACTOR_ID, consumerId);
      reqContext.put(JsonKey.ACTOR_TYPE, JsonKey.CONSUMER);
      ctx.flash().put(JsonKey.ACTOR_ID, consumerId);
      ctx.flash().put(JsonKey.ACTOR_TYPE, JsonKey.CONSUMER);
    }

    context.setRequestContext(reqContext);
    Map<String, Object> map = new HashMap<>();
    map.put(JsonKey.CONTEXT, TelemetryUtil.getTelemetryContext());
    Map<String, Object> additionalInfo = new HashMap<>();
    additionalInfo.put(JsonKey.URL, url);
    additionalInfo.put(JsonKey.METHOD, methodName);
    additionalInfo.put(JsonKey.START_TIME, startTime);

    // additional info contains info other than context info ...
    map.put(JsonKey.ADDITIONAL_INFO, additionalInfo);
    if (StringUtils.isBlank(messageId)) {
      messageId = JsonKey.DEFAULT_CONSUMER_ID;
    }
    ctx.flash().put(JsonKey.REQUEST_ID, messageId);
    requestInfo.put(messageId, map);
  }

  private String getEnv(Request request) {

    String uri = request.uri();
    String env;
    if (uri.startsWith("/v1/user") || uri.startsWith("/v2/user")) {
      env = JsonKey.USER;
    } else if (uri.startsWith("/v1/org")) {
      env = JsonKey.ORGANISATION;
    } else if (uri.startsWith("/v1/object")) {
      env = JsonKey.ANNOUNCEMENT;
    } else if (uri.startsWith("/v1/page")) {
      env = JsonKey.PAGE;
    } else if (uri.startsWith("/v1/course/batch")) {
      env = JsonKey.BATCH;
    } else if (uri.startsWith("/v1/notification")) {
      env = JsonKey.NOTIFICATION;
    } else if (uri.startsWith("/v1/dashboard")) {
      env = JsonKey.DASHBOARD;
    } else if (uri.startsWith("/v1/badges")) {
      env = JsonKey.BADGES;
    } else if (uri.startsWith("/v1/issuer")) {
      env = BadgingJsonKey.BADGES;
    } else if (uri.startsWith("/v1/content")) {
      env = JsonKey.BATCH;
    } else if (uri.startsWith("/v1/role")) {
      env = JsonKey.ROLE;
    } else if (uri.startsWith("/v1/note")) {
      env = JsonKey.NOTE;
    } else if (uri.startsWith("/v1/location")) {
      env = JsonKey.LOCATION;
    } else {
      env = "miscellaneous";
    }
    return env;
  }

  /**
   * This method will do request data validation for GET method only. As a GET request user must
   * send some key in header.
   *
   * @param request Request
   * @param errorMessage String
   * @return Promise<Result>
   */
  public Promise<Result> onDataValidationError(
      Request request, String errorMessage, int responseCode) {
    ProjectLogger.log("Data error found--" + errorMessage);
    ResponseCode code = ResponseCode.getResponse(errorMessage);
    ResponseCode headerCode = ResponseCode.CLIENT_ERROR;
    Response resp = BaseController.createFailureResponse(request, code, headerCode);
    return Promise.<Result>pure(Results.status(responseCode, Json.toJson(resp)));
  }

  /**
   * This method will be used to send the request header missing error message.
   *
   * @param request Http.RequestHeader
   * @param t Throwable
   * @return Promise<Result>
   */
  @Override
  public Promise<Result> onError(Http.RequestHeader request, Throwable t) {

    Response response = null;
    ProjectCommonException commonException = null;
    if (t instanceof ProjectCommonException) {
      commonException = (ProjectCommonException) t;
      response =
          BaseController.createResponseOnException(
              request.path(), request.method(), (ProjectCommonException) t);
    } else if (t instanceof akka.pattern.AskTimeoutException) {
      commonException =
          new ProjectCommonException(
              ResponseCode.actorConnectionError.getErrorCode(),
              ResponseCode.actorConnectionError.getErrorMessage(),
              ResponseCode.SERVER_ERROR.getResponseCode());
    } else {
      commonException =
          new ProjectCommonException(
              ResponseCode.internalError.getErrorCode(),
              ResponseCode.internalError.getErrorMessage(),
              ResponseCode.SERVER_ERROR.getResponseCode());
    }
    response =
        BaseController.createResponseOnException(request.path(), request.method(), commonException);
    return Promise.<Result>pure(Results.internalServerError(Json.toJson(response)));
  }

  /**
   * This method will identify the environment and update with enum.
   *
   * @return Environment
   */
  public Environment setEnvironment() {

    if (play.Play.isDev()) {
      return env = Environment.dev;
    } else if (play.Play.isTest()) {
      return env = Environment.qa;
    } else {
      return env = Environment.prod;
    }
  }

  /**
   * Method to get the response id on basis of request path.
   *
   * @param requestPath
   * @return
   */
  public static String getResponseId(String requestPath) {

    String path = requestPath;
    final String ver = "/" + version;
    final String ver2 = "/" + JsonKey.VERSION_2;
    path = path.trim();
    StringBuilder builder = new StringBuilder("");
    if (path.startsWith(ver) || path.startsWith(ver2)) {
      String requestUrl = (path.split("\\?"))[0];
      if (requestUrl.contains(ver)) {
        requestUrl = requestUrl.replaceFirst(ver, "api");
      } else {
        requestUrl = requestUrl.replaceFirst(ver2, "api");
      }

      String[] list = requestUrl.split("/");
      for (String str : list) {
        if (str.matches("[A-Za-z]+")) {
          builder.append(str).append(".");
        }
      }
      builder.deleteCharAt(builder.length() - 1);
    } else {
      if ("/health".equalsIgnoreCase(path)) {
        builder.append("api.all.health");
      }
    }
    return builder.toString();
  }
}
