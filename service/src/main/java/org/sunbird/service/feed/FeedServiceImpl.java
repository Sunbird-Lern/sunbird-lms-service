package org.sunbird.feed.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.exception.ProjectCommonException;
import org.sunbird.exception.ResponseCode;
import org.sunbird.feed.IFeedService;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.keys.JsonKey;
import org.sunbird.learner.util.Util;
import org.sunbird.logging.LoggerUtil;
import org.sunbird.models.user.Feed;
import org.sunbird.request.RequestContext;
import org.sunbird.response.Response;
import org.sunbird.util.ProjectUtil;

public class FeedServiceImpl implements IFeedService {
  private static LoggerUtil logger = new LoggerUtil(FeedServiceImpl.class);

  private Util.DbInfo usrFeedDbInfo = Util.dbInfoMap.get(JsonKey.USER_FEED_DB);
  private ObjectMapper mapper = new ObjectMapper();

  public static CassandraOperation getCassandraInstance() {
    return ServiceFactory.getInstance();
  }

  @Override
  public Response insert(Feed feed, RequestContext context) {
    logger.info(context, "FeedServiceImpl:insert method called : ");
    Map<String, Object> dbMap = mapper.convertValue(feed, Map.class);
    String feedId = ProjectUtil.generateUniqueId();
    dbMap.put(JsonKey.ID, feedId);
    dbMap.put(JsonKey.CREATED_ON, new Timestamp(Calendar.getInstance().getTimeInMillis()));
    try {
      if (MapUtils.isNotEmpty(feed.getData())) {
        dbMap.put(JsonKey.FEED_DATA, mapper.writeValueAsString(feed.getData()));
      }
    } catch (Exception ex) {
      logger.error(context, "FeedServiceImpl:insert Exception occurred while mapping.", ex);
      ProjectCommonException.throwServerErrorException(ResponseCode.SERVER_ERROR);
    }
    Response response =
        getCassandraInstance()
            .insertRecord(
                usrFeedDbInfo.getKeySpace(), usrFeedDbInfo.getTableName(), dbMap, context);
    return response;
  }

  @Override
  public Response update(Feed feed, RequestContext context) {
    logger.info(context, "FeedServiceImpl:update method called : ");
    Map<String, Object> dbMap = mapper.convertValue(feed, Map.class);
    try {
      if (MapUtils.isNotEmpty(feed.getData())) {
        dbMap.put(JsonKey.FEED_DATA, mapper.writeValueAsString(feed.getData()));
      }
    } catch (Exception ex) {
      logger.error(context, "FeedServiceImpl:update Exception occurred while mapping.", ex);
      ProjectCommonException.throwServerErrorException(ResponseCode.SERVER_ERROR);
    }
    dbMap.remove(JsonKey.CREATED_ON);
    dbMap.put(JsonKey.UPDATED_ON, new Timestamp(Calendar.getInstance().getTimeInMillis()));
    Response response = updateFeed(dbMap, context);
    return response;
  }

  @Override
  public List<Feed> getFeedsByProperties(Map<String, Object> properties, RequestContext context) {
    logger.info(context, "FeedServiceImpl:getFeedsByUserId method called : ");
    Response dbResponse =
        getCassandraInstance()
            .getRecordById(
                usrFeedDbInfo.getKeySpace(), usrFeedDbInfo.getTableName(), properties, context);
    List<Map<String, Object>> responseList = null;
    List<Feed> feedList = new ArrayList<>();
    if (null != dbResponse && null != dbResponse.getResult()) {
      responseList = (List<Map<String, Object>>) dbResponse.getResult().get(JsonKey.RESPONSE);
      if (CollectionUtils.isNotEmpty(responseList)) {
        responseList.forEach(
            s -> {
              try {
                String data = (String) s.get(JsonKey.FEED_DATA);
                if (StringUtils.isNotBlank(data)) {
                  s.put(
                      JsonKey.FEED_DATA,
                      mapper.readValue(data, new TypeReference<Map<String, Object>>() {}));
                } else {
                  s.put(JsonKey.FEED_DATA, Collections.emptyMap());
                }
                feedList.add(mapper.convertValue(s, Feed.class));
              } catch (Exception ex) {
                logger.error(
                    context,
                    "FeedServiceImpl:getRecordsByUserId :Exception occurred while mapping feed data.",
                    ex);
              }
            });
      }
    }
    return feedList;
  }

  @Override
  public void delete(String id, String userId, String category, RequestContext context) {
    logger.info(context, "FeedServiceImpl:delete method called for feedId : " + id);
    Map<String, String> compositeKey = new LinkedHashMap<>(3);
    compositeKey.put("userid", userId);
    compositeKey.put("category", category);
    compositeKey.put("id", id);
    getCassandraInstance()
        .deleteRecord(
            usrFeedDbInfo.getKeySpace(), usrFeedDbInfo.getTableName(), compositeKey, context);
  }

  private Response updateFeed(Map<String, Object> feed, RequestContext context) {
    Map<String, Object> compositeKey = new LinkedHashMap<>(3);
    compositeKey.put(JsonKey.USER_ID, feed.remove(JsonKey.USER_ID));
    compositeKey.put(JsonKey.CATEGORY, feed.remove(JsonKey.CATEGORY));
    compositeKey.put(JsonKey.ID, feed.remove(JsonKey.ID));
    return getCassandraInstance()
        .updateRecord(
            usrFeedDbInfo.getKeySpace(), usrFeedDbInfo.getTableName(), feed, compositeKey, context);
  }
}
