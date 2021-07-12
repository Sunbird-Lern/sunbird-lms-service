package org.sunbird.location.dao;

import java.util.Map;
import org.sunbird.models.location.Location;
import org.sunbird.request.RequestContext;
import org.sunbird.response.Response;

/** @author Amit Kumar */
public interface LocationDao {
  /**
   * @param location Location Details
   * @param context
   * @return response Response
   */
  Response create(Location location, RequestContext context);

  /**
   * @param location Location Details
   * @param context
   * @return response Response
   */
  Response update(Location location, RequestContext context);

  /**
   * @param locationId its a unique identity for Location
   * @param context
   * @return response Response
   */
  Response delete(String locationId, RequestContext context);

  /**
   * @param searchQueryMap Map<String,Object> it contains the filters to search Location from ES
   * @param context
   * @return response Response
   */
  Response search(Map<String, Object> searchQueryMap, RequestContext context);

  /**
   * @param locationId
   * @param context
   * @return response Response
   */
  Response read(String locationId, RequestContext context);

  /**
   * @param queryMap
   * @param context
   * @return response Response
   */
  Response getRecordByProperty(Map<String, Object> queryMap, RequestContext context);
}
