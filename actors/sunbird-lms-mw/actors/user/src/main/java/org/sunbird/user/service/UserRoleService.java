package org.sunbird.user.service;

import java.util.List;
import java.util.Map;
import org.sunbird.common.request.RequestContext;

public interface UserRoleService {
  List<Map<String, Object>> updateUserRole(Map userRequest, RequestContext context);
}
