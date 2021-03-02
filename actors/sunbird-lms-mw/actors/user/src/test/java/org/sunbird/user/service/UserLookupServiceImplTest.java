package org.sunbird.user.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.request.RequestContext;
import org.sunbird.models.user.User;
import org.sunbird.user.dao.impl.UserLookupDaoImpl;
import org.sunbird.user.service.impl.UserLookUpServiceImpl;

@RunWith(PowerMockRunner.class)
@PrepareForTest({UserLookupDaoImpl.class})
@PowerMockIgnore({
  "javax.management.*",
  "javax.net.ssl.*",
  "javax.security.*",
  "jdk.internal.reflect.*",
  "javax.crypto.*"
})
public class UserLookupServiceImplTest {

  @Test
  public void checkEmailUniquenessValid() {
    PowerMockito.mockStatic(UserLookupDaoImpl.class);
    UserLookupDaoImpl userLookupDao = PowerMockito.mock(UserLookupDaoImpl.class);
    PowerMockito.when(UserLookupDaoImpl.getInstance()).thenReturn(userLookupDao);
    PowerMockito.when(userLookupDao.getEmailByType(Mockito.any(), Mockito.any()))
        .thenReturn(getRecords());
    UserLookupService userLookupService = UserLookUpServiceImpl.getInstance();
    User user = new User();
    user.setId("1234");
    user.setEmail("abc@xzn");
    userLookupService.checkEmailUniqueness(user, "read", new RequestContext());
  }

  private List<Map<String, Object>> getRecords() {
    List<Map<String, Object>> result = new ArrayList<>();
    Map<String, Object> mapObj = new HashMap<>();
    mapObj.put(JsonKey.USER_ID, "1234");
    result.add(mapObj);
    return result;
  }
}
