package controllers.storage;

import com.fasterxml.jackson.databind.JsonNode;
import controllers.BaseController;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import org.apache.commons.io.IOUtils;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.request.Request;
import org.sunbird.common.responsecode.ResponseCode;
import play.libs.Files;
import play.mvc.Http;
import play.mvc.Http.MultipartFormData;
import play.mvc.Http.MultipartFormData.FilePart;
import play.mvc.Result;

/** Created by arvind on 28/8/17. */
public class FileStorageController extends BaseController {

  /**
   * This method to upload the files on cloud storage .
   *
   * @return CompletionStage<Result>
   */
  public CompletionStage<Result> uploadFileService(Http.Request httpRequest) {

    try {

      Request reqObj = new Request();
      Map<String, Object> map = new HashMap<>();
      byte[] byteArray = null;
      MultipartFormData body = httpRequest.body().asMultipartFormData();
      Map<String, String[]> formUrlEncodeddata = httpRequest.body().asFormUrlEncoded();
      JsonNode requestData = httpRequest.body().asJson();
      if (body != null) {
        Map<String, String[]> data = body.asFormUrlEncoded();
        for (Entry<String, String[]> entry : data.entrySet()) {
          map.put(entry.getKey(), entry.getValue()[0]);
        }
        List<FilePart<Files.TemporaryFile>> filePart = body.getFiles();
        File f = filePart.get(0).getRef().path().toFile();

        InputStream is = new FileInputStream(f);
        byteArray = IOUtils.toByteArray(is);
        reqObj.getRequest().putAll(map);
        map.put(JsonKey.FILE_NAME, filePart.get(0).getFilename());
      } else if (null != formUrlEncodeddata) {
        // read data as string from request
        for (Entry<String, String[]> entry : formUrlEncodeddata.entrySet()) {
          map.put(entry.getKey(), entry.getValue()[0]);
        }
        InputStream is =
            new ByteArrayInputStream(
                ((String) map.get(JsonKey.DATA)).getBytes(StandardCharsets.UTF_8));
        byteArray = IOUtils.toByteArray(is);
        reqObj.getRequest().putAll(map);
      } else if (null != requestData) {
        reqObj =
            (Request) mapper.RequestMapper.mapRequest(httpRequest.body().asJson(), Request.class);
        InputStream is =
            new ByteArrayInputStream(
                ((String) reqObj.getRequest().get(JsonKey.DATA)).getBytes(StandardCharsets.UTF_8));
        byteArray = IOUtils.toByteArray(is);
        reqObj.getRequest().putAll(map);
        map.putAll(reqObj.getRequest());
      } else {
        ProjectCommonException e =
            new ProjectCommonException(
                ResponseCode.invalidData.getErrorCode(),
                ResponseCode.invalidData.getErrorMessage(),
                ResponseCode.CLIENT_ERROR.getResponseCode());
        return CompletableFuture.completedFuture(createCommonExceptionResponse(e, httpRequest));
      }
      reqObj.setOperation(ActorOperations.FILE_STORAGE_SERVICE.getValue());
      reqObj.setRequestId(httpRequest.flash().get(JsonKey.REQUEST_ID));
      reqObj.setEnv(getEnvironment());
      HashMap<String, Object> innerMap = new HashMap<>();
      innerMap.put(JsonKey.DATA, map);
      map.put(JsonKey.CREATED_BY, httpRequest.flash().get(JsonKey.USER_ID));
      reqObj.setRequest(innerMap);
      map.put(JsonKey.FILE, byteArray);

      return actorResponseHandler(getActorRef(), reqObj, timeout, null, httpRequest);
    } catch (Exception e) {
      return CompletableFuture.completedFuture(createCommonExceptionResponse(e, httpRequest));
    }
  }
}
