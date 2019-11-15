package controllers.usermanagement.validator;

import org.junit.*;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.request.Request;

import java.util.HashMap;
import java.util.Map;

public class ShadowUserMigrateReqValidatorTest {

    private Request request;
    private ShadowUserMigrateReqValidator shadowUserMigrateReqValidator;


    @Before
    public void setUp() throws Exception {
        request=new Request();

    }

    @Test
    public void testMigrateReqWithoutMandatoryParamExternalId() {
        Map<String,Object> reqMap=new HashMap<>();
        reqMap.put(JsonKey.USER_ID,"abc");
        reqMap.put(JsonKey.ACTION,"accept");
        reqMap.put(JsonKey.CHANNEL,"TN");
        request.setRequest(reqMap);
        shadowUserMigrateReqValidator=ShadowUserMigrateReqValidator.getInstance(request,"abc");
        try {
            shadowUserMigrateReqValidator.validate();
        }
        catch (Exception e){
            Assert.assertEquals("Data type of userExtId should be String.",e.getMessage());
        }
    }

    @Test
    public void testMigrateReqWithoutMandatoryParamAction() {
        Map<String,Object> reqMap=new HashMap<>();
        reqMap.put(JsonKey.USER_ID,"abc");
        reqMap.put(JsonKey.USER_EXT_ID,"abc_ext_id");
        reqMap.put(JsonKey.CHANNEL,"TN");
        request.setRequest(reqMap);
        shadowUserMigrateReqValidator=ShadowUserMigrateReqValidator.getInstance(request,"abc");
        try {
            shadowUserMigrateReqValidator.validate();
        }
        catch (Exception e){
            Assert.assertEquals("Data type of action should be String.",e.getMessage());
        }    }
    @Test
    public void testMigrateReqWithoutMandatoryParamUserId() {
        Map<String,Object> reqMap=new HashMap<>();
        reqMap.put(JsonKey.USER_EXT_ID,"abc_ext_id");
        reqMap.put(JsonKey.CHANNEL,"TN");
        reqMap.put(JsonKey.ACTION,"accept");
        request.setRequest(reqMap);
        shadowUserMigrateReqValidator=ShadowUserMigrateReqValidator.getInstance(request,"abc");
        try {
            shadowUserMigrateReqValidator.validate();
        }
        catch (Exception e){
            Assert.assertEquals("Data type of userId should be String.",e.getMessage());
        }    }

    @Test
    public void testMigrateReqWithInvalidValueAction() {
        Map<String,Object> reqMap=new HashMap<>();
        reqMap.put(JsonKey.USER_ID,"abc");
        reqMap.put(JsonKey.USER_EXT_ID,"abc_ext_id");
        reqMap.put(JsonKey.CHANNEL,"TN");
        reqMap.put(JsonKey.ACTION,"action_incorrect_value");
        request.setRequest(reqMap);
        shadowUserMigrateReqValidator=ShadowUserMigrateReqValidator.getInstance(request,"abc");
        try {
            shadowUserMigrateReqValidator.validate();
        }
        catch (Exception e){
            Assert.assertEquals("Invalid value action_incorrect_value for parameter action supported actions are:[accept, reject]. Please provide a valid value.",e.getMessage());
        }    }
    @Test
    public void testMigrateReqWithDiffCallerId() {
        Map<String,Object> reqMap=new HashMap<>();
        reqMap.put(JsonKey.USER_EXT_ID,"abc_ext_id");
        reqMap.put(JsonKey.USER_ID,"abc");
        reqMap.put(JsonKey.CHANNEL,"TN");
        reqMap.put(JsonKey.ACTION,"accept");
        request.setRequest(reqMap);
        shadowUserMigrateReqValidator=ShadowUserMigrateReqValidator.getInstance(request,"abcD");
        try {
            shadowUserMigrateReqValidator.validate();
        }
        catch (Exception e){
            Assert.assertEquals("Invalid value abc for parameter userId. Please provide a valid value.",e.getMessage());
        }    }
    @Test()
    public void testMigrateReqSuccess() {
        Map<String,Object> reqMap=new HashMap<>();
        reqMap.put(JsonKey.USER_EXT_ID,"abc_ext_id");
        reqMap.put(JsonKey.USER_ID,"abc");
        reqMap.put(JsonKey.CHANNEL,"TN");
        reqMap.put(JsonKey.ACTION,"accept");
        request.setRequest(reqMap);
        shadowUserMigrateReqValidator=ShadowUserMigrateReqValidator.getInstance(request,"abc");
        shadowUserMigrateReqValidator.validate();
    }
}