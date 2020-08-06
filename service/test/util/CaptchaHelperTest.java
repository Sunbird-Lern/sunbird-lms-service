package util;

import static org.junit.Assert.*;
import static org.powermock.api.mockito.PowerMockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.sunbird.common.models.util.HttpClientUtil;
import org.sunbird.common.models.util.ProjectUtil;

@RunWith(PowerMockRunner.class)
@PrepareForTest({HttpClientUtil.class, ProjectUtil.class})
@PowerMockIgnore({"javax.management.*"})
public class CaptchaHelperTest {

  @Test
  public void testCaptchaHelper() {
    boolean isValidate = new CaptchaHelper().validate("5ASD", null);
    assertTrue(isValidate);
  }

  @Test
  public void testCaptchaHelperForPortal() {
    boolean isValidate = new CaptchaHelper().validate("5ASD", "portal");
    assertTrue(isValidate);
  }

  @Before
  public void setup() throws JsonProcessingException {
    PowerMockito.mockStatic(HttpClientUtil.class);
    PowerMockito.mockStatic(ProjectUtil.class);
    Map map = new HashMap<String, String>();
    map.put("success", true);
    ObjectMapper objectMapper = new ObjectMapper();
    String s = objectMapper.writeValueAsString(map);
    when(ProjectUtil.getConfigValue(Mockito.anyString())).thenReturn("anyString");
    when(HttpClientUtil.postFormData(Mockito.anyString(), Mockito.anyMap(), Mockito.anyMap()))
        .thenReturn(s);
  }
}
