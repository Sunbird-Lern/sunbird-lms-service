package util;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.response.ResponseParams;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.request.Request;
import org.sunbird.common.request.RequestContext;
import org.sunbird.common.responsecode.ResponseCode;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Common.class})
@PowerMockIgnore({"javax.management.*", "javax.net.ssl.*", "javax.security.*"})
public class PrintEntryExitLogTest {
  @Test
  public void testPrintExitLogOnFailure() {
    try {
      ResponseParams params = new ResponseParams();
      ProjectCommonException exception =
          new ProjectCommonException(
              ResponseCode.internalError.getErrorCode(),
              ResponseCode.internalError.getErrorMessage(),
              ResponseCode.SERVER_ERROR.getResponseCode());
      ResponseCode code = ResponseCode.getResponse(exception.getCode());
      params.setErr(code.getErrorCode());
      params.setErrmsg(code.getErrorMessage());
      params.setStatus(JsonKey.FAILED);
      params.setMsgid("123-456-789");
      PowerMockito.mockStatic(Common.class);
      PowerMockito.when(
              Common.createResponseParamObj(
                  Mockito.any(ResponseCode.class), Mockito.anyString(), Mockito.anyString()))
          .thenReturn(params);
      Request request = new Request();
      request.getContext().put(JsonKey.METHOD, "POST");
      request.getContext().put(JsonKey.URL, "/private/user/v1/lookup");
      request.setOperation("searchUser");
      RequestContext requestContext = new RequestContext();
      requestContext.setReqId("123-456-789");
      request.setRequestContext(requestContext);
      PrintEntryExitLog.printExitLogOnFailure(request, null);
      Assert.assertNotNull(request);
    } catch (Exception e) {
      Assert.assertNull(e);
    }
  }
}
