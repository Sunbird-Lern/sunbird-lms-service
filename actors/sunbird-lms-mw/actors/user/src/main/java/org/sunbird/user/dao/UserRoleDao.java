package org.sunbird.user.dao;

import java.util.List;
import java.util.Map;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.request.RequestContext;

public interface UserRoleDao {

  Response assignUserRole(List<Map<String, Object>> userRoleMap, RequestContext context);

  Response updateRoleScope(List<Map<String, Object>> userRoleMap, RequestContext context);

  void deleteUserRole(List<Map<String, String>> userRoleMap, RequestContext context);

  List<Map<String, Object>> getUserRoles(String userId, String role, RequestContext context);
}
