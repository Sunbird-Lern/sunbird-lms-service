package org.sunbird.service.otp;

import java.io.StringWriter;
import java.util.Map;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.runtime.RuntimeServices;
import org.apache.velocity.runtime.RuntimeSingleton;
import org.apache.velocity.runtime.parser.node.SimpleNode;
import org.sunbird.dao.notification.EmailTemplateDao;
import org.sunbird.dao.notification.impl.EmailTemplateDaoImpl;
import org.sunbird.dao.otp.OTPDao;
import org.sunbird.dao.otp.impl.OTPDaoImpl;
import org.sunbird.keys.JsonKey;
import org.sunbird.logging.LoggerUtil;
import org.sunbird.request.RequestContext;
import org.sunbird.service.user.UserService;
import org.sunbird.service.user.impl.UserServiceImpl;
import org.sunbird.util.ProjectUtil;

public class OTPService {
  private static LoggerUtil logger = new LoggerUtil(OTPService.class);

  private OTPDao otpDao = OTPDaoImpl.getInstance();
  private EmailTemplateDao emailTemplateDao = EmailTemplateDaoImpl.getInstance();
  private UserService userService = UserServiceImpl.getInstance();

  public Map<String, Object> getOTPDetails(String type, String key, RequestContext context) {
    return otpDao.getOTPDetails(type, key, context);
  }

  public void insertOTPDetails(String type, String key, String otp, RequestContext context) {
    otpDao.insertOTPDetails(type, key, otp, context);
  }

  public void deleteOtp(String type, String key, RequestContext context) {
    otpDao.deleteOtp(type, key, context);
  }

  /**
   * This method will return either email or phone value of user based on the asked type in request
   *
   * @param userId
   * @param type value can be email, phone, recoveryEmail, recoveryPhone , prevUsedEmail or prevUsedPhone
   * @return
   */
  public String getEmailPhoneByUserId(String userId, String type, RequestContext context) {
    return userService.getDecryptedEmailPhoneByUserId(userId, type, context);
  }

  public String getSmsBody(
      String templateFile, Map<String, String> smsTemplate, RequestContext requestContext) {
    try {
      String sms = emailTemplateDao.getTemplate(templateFile, requestContext);
      RuntimeServices rs = RuntimeSingleton.getRuntimeServices();
      SimpleNode sn = rs.parse(sms, "Sms Information");
      Template t = new Template();
      t.setRuntimeServices(rs);
      t.setData(sn);
      t.initDocument();
      VelocityContext context = new VelocityContext();
      context.put(JsonKey.OTP, smsTemplate.get(JsonKey.OTP));
      context.put(
          JsonKey.OTP_EXPIRATION_IN_MINUTES, smsTemplate.get(JsonKey.OTP_EXPIRATION_IN_MINUTES));
      context.put(
          JsonKey.INSTALLATION_NAME,
          ProjectUtil.getConfigValue(JsonKey.SUNBIRD_INSTALLATION_DISPLAY_NAME));

      StringWriter writer = new StringWriter();
      t.merge(context, writer);
      return writer.toString();
    } catch (Exception ex) {
      logger.error(
          requestContext,
          "OTPService:getSmsBody: Exception occurred with error message = " + ex.getMessage(),
          ex);
    }
    return "";
  }

  public void updateAttemptCount(Map<String, Object> otpDetails, RequestContext context) {
    otpDao.updateAttemptCount(otpDetails, context);
  }
}
