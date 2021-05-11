package org.sunbird.learner.util;

import java.util.*;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.common.models.util.*;
import org.sunbird.common.models.util.datasecurity.DataMaskingService;
import org.sunbird.common.models.util.datasecurity.DecryptionService;
import org.sunbird.common.models.util.datasecurity.EncryptionService;
import org.sunbird.common.models.util.datasecurity.impl.ServiceFactory;

/**
 * This class is for utility methods for encrypting user data.
 *
 * @author Amit Kumar
 */
public final class UserUtility {
  private static LoggerUtil logger = new LoggerUtil(UserUtility.class);

  private static List<String> userKeyToEncrypt;
  private static List<String> userKeyToDecrypt;
  private static List<String> userKeysToMasked;
  private static DecryptionService decryptionService;
  private static DataMaskingService maskingService;
  private static List<String> phoneMaskedAttributes;
  private static List<String> emailMaskedAttributes;

  static {
    init();
  }

  private UserUtility() {}

  public static Map<String, Object> encryptUserData(Map<String, Object> userMap) throws Exception {
    return encryptSpecificUserData(userMap, userKeyToEncrypt);
  }

  @SuppressWarnings("unchecked")
  public static Map<String, Object> encryptSpecificUserData(
      Map<String, Object> userMap, List<String> fieldsToEncrypt) throws Exception {
    EncryptionService service = ServiceFactory.getEncryptionServiceInstance(null);
    // Encrypt user basic info
    for (String key : fieldsToEncrypt) {
      if (userMap.containsKey(key)) {
        userMap.put(key, service.encryptData((String) userMap.get(key), null));
      }
    }
    return userMap;
  }

  public static Map<String, Object> decryptUserData(Map<String, Object> userMap) {
    return decryptSpecificUserData(userMap, userKeyToEncrypt);
  }

  public static Map<String, Object> decryptSpecificUserData(
      Map<String, Object> userMap, List<String> fieldsToDecrypt) {
    DecryptionService service = ServiceFactory.getDecryptionServiceInstance(null);
    // Decrypt user basic info
    for (String key : fieldsToDecrypt) {
      if (userMap.containsKey(key)) {
        userMap.put(key, service.decryptData((String) userMap.get(key), null));
      }
    }
    return userMap;
  }

  public static boolean isMasked(String data) {
    return maskingService.isMasked(data);
  }

  public static Map<String, Object> decryptUserDataFrmES(Map<String, Object> userMap) {
    DecryptionService service = ServiceFactory.getDecryptionServiceInstance(null);
    // Decrypt user basic info
    for (String key : userKeyToDecrypt) {
      if (userMap.containsKey(key)) {
        if (userKeysToMasked.contains(key)) {
          userMap.put(key, maskEmailOrPhone((String) userMap.get(key), key));
        } else {
          userMap.put(key, service.decryptData((String) userMap.get(key), null));
        }
      }
    }
    return userMap;
  }

  public static Map<String, Object> encryptUserSearchFilterQueryData(Map<String, Object> map)
      throws Exception {
    Map<String, Object> filterMap = (Map<String, Object>) map.get(JsonKey.FILTERS);
    EncryptionService service = ServiceFactory.getEncryptionServiceInstance(null);
    // Encrypt user basic info
    for (String key : userKeyToEncrypt) {
      if (filterMap.containsKey(key)) {
        filterMap.put(key, service.encryptData((String) filterMap.get(key), null));
      }
    }
    return filterMap;
  }

  public static String encryptData(String data) throws Exception {
    EncryptionService service = ServiceFactory.getEncryptionServiceInstance(null);
    return service.encryptData(data, null);
  }

  public static String maskEmailOrPhone(String encryptedEmailOrPhone, String type) {
    if (StringUtils.isEmpty(encryptedEmailOrPhone)) {
      return StringUtils.EMPTY;
    }
    if (phoneMaskedAttributes.contains(type)) {
      return maskingService.maskPhone(decryptionService.decryptData(encryptedEmailOrPhone, null));
    } else if (emailMaskedAttributes.contains(type)) {
      return maskingService.maskEmail(decryptionService.decryptData(encryptedEmailOrPhone, null));
    }
    return StringUtils.EMPTY;
  }

  private static void init() {
    decryptionService =
        org.sunbird.common.models.util.datasecurity.impl.ServiceFactory
            .getDecryptionServiceInstance(null);
    maskingService =
        org.sunbird.common.models.util.datasecurity.impl.ServiceFactory.getMaskingServiceInstance(
            null);
    String userKey = PropertiesCache.getInstance().getProperty("userkey.encryption");
    userKeyToEncrypt = new ArrayList<>(Arrays.asList(userKey.split(",")));
    logger.info("UserUtility:init:user encrypt  attributes got".concat(userKey + ""));
    String userKeyDecrypt = PropertiesCache.getInstance().getProperty("userkey.decryption");
    String userKeyToMasked = PropertiesCache.getInstance().getProperty("userkey.masked");
    userKeyToDecrypt = new ArrayList<>(Arrays.asList(userKeyDecrypt.split(",")));
    userKeysToMasked = new ArrayList<>(Arrays.asList(userKeyToMasked.split(",")));
    String emailTypeAttributeKey =
        PropertiesCache.getInstance().getProperty("userkey.emailtypeattributes");
    String phoneTypeAttributeKey =
        PropertiesCache.getInstance().getProperty("userkey.phonetypeattributes");
    emailMaskedAttributes = new ArrayList<>(Arrays.asList(emailTypeAttributeKey.split(",")));
    logger.info("UserUtility:init:email masked attributes got".concat(emailTypeAttributeKey + ""));
    phoneMaskedAttributes = new ArrayList<>(Arrays.asList(phoneTypeAttributeKey.split(",")));
    logger.info("UserUtility:init:phone masked attributes got".concat(phoneTypeAttributeKey + ""));
  }
}
