package org.sunbird.notification.sms;

import static org.powermock.api.mockito.PowerMockito.doReturn;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.sunbird.notification.sms.providerimpl.Msg91SmsProvider;
import org.sunbird.notification.utils.PropertiesCache;
import org.sunbird.notification.utils.SMSFactory;
import org.sunbird.notification.utils.SmsTemplateUtil;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"javax.management.*", "javax.net.ssl.*", "javax.security.*"})
@PrepareForTest({
  HttpClients.class,
  CloseableHttpClient.class,
  PropertiesCache.class,
  SMSFactory.class,
  SmsTemplateUtil.class
})
public class Msg91PostMethodTest {

  private void initMockRulesFor200() {
    CloseableHttpClient httpClient = mock(CloseableHttpClient.class);
    CloseableHttpResponse httpResp = mock(CloseableHttpResponse.class);
    PropertiesCache propertiesCache = mock(PropertiesCache.class);
    StatusLine statusLine = mock(StatusLine.class);
    PowerMockito.mockStatic(HttpClients.class);
    try {
      doReturn(httpClient).when(HttpClients.class, "createDefault");
      when(httpClient.execute(Mockito.any(HttpPost.class))).thenReturn(httpResp);
      doReturn(statusLine).when(httpResp).getStatusLine();
      doReturn(200).when(statusLine).getStatusCode();
    } catch (Exception e) {
      Assert.fail("Exception while mocking static " + e.getLocalizedMessage());
    }

    try {
      PowerMockito.when(propertiesCache.getProperty(Mockito.anyString())).thenReturn("anyString");
    } catch (Exception e) {
      Assert.fail("Exception while mocking static " + e.getLocalizedMessage());
    }
  }

  private void initMockRulesFor400() {
    CloseableHttpClient httpClient = mock(CloseableHttpClient.class);
    CloseableHttpResponse httpResp = mock(CloseableHttpResponse.class);
    PropertiesCache propertiesCache = mock(PropertiesCache.class);
    StatusLine statusLine = mock(StatusLine.class);
    PowerMockito.mockStatic(HttpClients.class);
    try {
      doReturn(httpClient).when(HttpClients.class, "createDefault");
      when(httpClient.execute(Mockito.any(HttpPost.class))).thenReturn(httpResp);
      doReturn(statusLine).when(httpResp).getStatusLine();
      doReturn(400).when(statusLine).getStatusCode();
    } catch (Exception e) {
      Assert.fail("Exception while mocking static " + e.getLocalizedMessage());
    }

    try {
      PowerMockito.when(propertiesCache.getProperty(Mockito.anyString())).thenReturn("anyString");
    } catch (Exception e) {
      Assert.fail("Exception while mocking static " + e.getLocalizedMessage());
    }
  }

  @Test
  public void testSendSms() {
    initMockRulesFor200();
    PowerMockito.mockStatic(SmsTemplateUtil.class);

    Map<String, String> template1 = new HashMap<>();
    template1.put(
        "OTP to verify your phone number on $installationName is $otp. This is valid for $otpExpiryInMinutes minutes only.",
        "1");
    template1.put(
        "OTP to reset your password on $installationName is $otp. This is valid for $otpExpiryInMinutes minutes only.",
        "2");
    template1.put(
        "Your ward has requested for registration on $installationName using this phone number. Use OTP $otp to agree and create the account. This is valid for $otpExpiryInMinutes minutes only.",
        "3");

    when(SmsTemplateUtil.getSmsTemplateConfigMap()).thenReturn(template1);
    Msg91SmsProvider megObj = new Msg91SmsProvider();
    String sms =
        "OTP to reset your password on instance is 456123. This is valid for 30 minutes only.";
    boolean response = megObj.send("4321111111", sms);
    Assert.assertTrue(response);
  }

  @Test
  public void testSendSmsFailure() {
    initMockRulesFor400();
    PowerMockito.mockStatic(SmsTemplateUtil.class);

    Map<String, String> template1 = new HashMap<>();
    template1.put(
        "OTP to verify your phone number on $installationName is $otp. This is valid for $otpExpiryInMinutes minutes only.",
        "1");
    template1.put(
        "OTP to reset your password on $installationName is $otp. This is valid for $otpExpiryInMinutes minutes only.",
        "2");
    template1.put(
        "Your ward has requested for registration on $installationName using this phone number. Use OTP $otp to agree and create the account. This is valid for $otpExpiryInMinutes minutes only.",
        "3");

    when(SmsTemplateUtil.getSmsTemplateConfigMap()).thenReturn(template1);
    Msg91SmsProvider megObj = new Msg91SmsProvider();
    String sms =
        "OTP to verify your phone number on instance is 456123. This is valid for 30 minutes only.";
    boolean response = megObj.send("4321111111", sms);
    Assert.assertFalse(response);
  }

  @Test
  public void testSendSmsToMultiplePhone() {
    initMockRulesFor200();
    PowerMockito.mockStatic(SmsTemplateUtil.class);

    Map<String, String> template1 = new HashMap<>();
    template1.put(
        "OTP to verify your phone number on $installationName is $otp. This is valid for $otpExpiryInMinutes minutes only.",
        "1");
    template1.put(
        "OTP to reset your password on $installationName is $otp. This is valid for $otpExpiryInMinutes minutes only.",
        "2");
    template1.put(
        "Your ward has requested for registration on $installationName using this phone number. Use OTP $otp to agree and create the account. This is valid for $otpExpiryInMinutes minutes only.",
        "3");

    when(SmsTemplateUtil.getSmsTemplateConfigMap()).thenReturn(template1);
    Msg91SmsProvider megObj = new Msg91SmsProvider();
    String sms =
        "OTP to verify your phone number on instance is 456123. This is valid for 30 minutes only.";
    List<String> phoneList = new ArrayList<>();
    phoneList.add("5464654653");
    phoneList.add("7897951543");
    boolean response = megObj.send(phoneList, sms);
    Assert.assertTrue(response);
  }

  @Test
  public void testSendSmsFailureToMultiplePhone() {
    initMockRulesFor400();
    PowerMockito.mockStatic(SmsTemplateUtil.class);

    Map<String, String> template1 = new HashMap<>();
    template1.put(
        "OTP to verify your phone number on $installationName is $otp. This is valid for $otpExpiryInMinutes minutes only.",
        "1");
    template1.put(
        "OTP to reset your password on $installationName is $otp. This is valid for $otpExpiryInMinutes minutes only.",
        "2");
    template1.put(
        "Your ward has requested for registration on $installationName using this phone number. Use OTP $otp to agree and create the account. This is valid for $otpExpiryInMinutes minutes only.",
        "3");

    when(SmsTemplateUtil.getSmsTemplateConfigMap()).thenReturn(template1);
    Msg91SmsProvider megObj = new Msg91SmsProvider();
    String sms =
        "OTP to verify your phone number on instance is 456123. This is valid for 30 minutes only.";
    List<String> phoneList = new ArrayList<>();
    phoneList.add("5464654653");
    phoneList.add("7897951543");
    boolean response = megObj.send(phoneList, sms);
    Assert.assertFalse(response);
  }

  @Test
  public void testSendSmsWithCountryCode() {
    initMockRulesFor200();
    PowerMockito.mockStatic(SmsTemplateUtil.class);

    Map<String, String> template1 = new HashMap<>();
    template1.put(
        "OTP to verify your phone number on $installationName is $otp. This is valid for $otpExpiryInMinutes minutes only.",
        "1");
    template1.put(
        "OTP to reset your password on $installationName is $otp. This is valid for $otpExpiryInMinutes minutes only.",
        "2");
    template1.put(
        "Your ward has requested for registration on $installationName using this phone number. Use OTP $otp to agree and create the account. This is valid for $otpExpiryInMinutes minutes only.",
        "3");

    when(SmsTemplateUtil.getSmsTemplateConfigMap()).thenReturn(template1);
    Msg91SmsProvider megObj = new Msg91SmsProvider();
    String sms =
        "OTP to verify your phone number on instance is 456123. This is valid for 30 minutes only.";
    boolean response = megObj.send("4321111111", "+91", sms);
    Assert.assertTrue(response);
  }

  @Test
  public void testSendSmsFailureWithCountryCode() {
    initMockRulesFor400();
    PowerMockito.mockStatic(SmsTemplateUtil.class);

    Map<String, String> template1 = new HashMap<>();
    template1.put(
        "OTP to verify your phone number on $installationName is $otp. This is valid for $otpExpiryInMinutes minutes only.",
        "1");
    template1.put(
        "OTP to reset your password on $installationName is $otp. This is valid for $otpExpiryInMinutes minutes only.",
        "2");
    template1.put(
        "Your ward has requested for registration on $installationName using this phone number. Use OTP $otp to agree and create the account. This is valid for $otpExpiryInMinutes minutes only.",
        "3");

    when(SmsTemplateUtil.getSmsTemplateConfigMap()).thenReturn(template1);
    Msg91SmsProvider megObj = new Msg91SmsProvider();
    String sms =
        "OTP to verify your phone number on instance is 456123. This is valid for 30 minutes only.";
    boolean response = megObj.send("4321111111", "+91", sms);
    Assert.assertFalse(response);
  }
}
