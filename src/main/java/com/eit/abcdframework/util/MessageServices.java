package com.eit.abcdframework.util;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.eit.abcdframework.globalhandler.GlobalAttributeHandler;
import com.eit.abcdframework.http.caller.Httpclientcaller;

@Service
public class MessageServices {

	private static final Logger LOGGER = LoggerFactory.getLogger(MessageServices.class);

	static Httpclientcaller dataTransmit;

	@Autowired
	public void setProductService(Httpclientcaller dataTransmit) {
		MessageServices.dataTransmit = dataTransmit;
	}


	static String Msegat_url = "https://www.msegat.com/gw/";
	private static final String BASE_URL_VERIFY = Msegat_url + "verifyOTPCode.php";
	private static final String BASE_URL_SENDOTP = Msegat_url + "sendOTPCode.php";
	private static final String BASE_URL_SMS = Msegat_url + "sendsms.php";

	private static final String userName = "REFA";
	private static final String apiKey = "ADAFFE70B1A089B84E1F934ACA646864";
	private static final String userSender = "TSHIL";
	

	public String MsegatOTPService(String reqBody) {
		try {
			JSONObject msgObject = new JSONObject();
			JSONObject object = new JSONObject(reqBody);

			JSONObject jsonInput = new JSONObject();
			jsonInput.put("lang", object.get("language"));
			jsonInput.put("userName", userName);
			jsonInput.put("apiKey", apiKey);
			jsonInput.put("number", object.get("phoneNo"));
			jsonInput.put("userSender", userSender);

			JSONObject jsonResponse = new JSONObject(
					dataTransmit.transmitDataspgrestpost(BASE_URL_SENDOTP, jsonInput.toString(), false,GlobalAttributeHandler.getSchemas()));
			if (jsonResponse.get("code").equals(1)) {
				msgObject.put("message", "Success");
				msgObject.put("id", jsonResponse.get("id"));
				return msgObject.toString();
			} else {
				return msgObject.put("message", jsonResponse.getString("message")).toString();
			}

		} catch (Exception e) {
			LOGGER.error(Thread.currentThread().getStackTrace()[0].getMethodName(), e);
			return "Something went wrong";
		}
	}

	public String MsegatverifyOTP(String reqBody) {
		try {
			JSONObject msgObject = new JSONObject();
			JSONObject object = new JSONObject(reqBody);

			JSONObject jsonInput = new JSONObject();
			jsonInput.put("lang", object.get("language"));
			jsonInput.put("userName", userName);
			jsonInput.put("apiKey", apiKey);
			jsonInput.put("code", object.get("OTP"));
			jsonInput.put("id", object.get("verificationId"));
			jsonInput.put("userSender", userSender);

			JSONObject jsonResponse = new JSONObject(
					dataTransmit.transmitDataspgrestpost(BASE_URL_VERIFY, jsonInput.toString(), false,GlobalAttributeHandler.getSchemas()));
			if (jsonResponse.get("code").equals(1)) {
				return msgObject.put("message", "Success").toString();
			} else {
				return msgObject.put("message", jsonResponse.getString("message")).toString();
			}

		} catch (Exception e) {
			LOGGER.error(Thread.currentThread().getStackTrace()[0].getMethodName(), e);
			return "Something went wrong";
		}
	}

	public static String MsegatsmsService(String moblie, String msg) {
		try {
			JSONObject msgObject = new JSONObject();

			JSONObject jsonInput = new JSONObject();
			jsonInput.put("numbers", moblie.replace("+", ""));
			jsonInput.put("userName", userName);
			jsonInput.put("apiKey", apiKey);
			jsonInput.put("msg", msg);
			jsonInput.put("userSender", userSender);

			JSONObject jsonResponse = new JSONObject(
					dataTransmit.transmitDataspgrestpost(BASE_URL_SMS, jsonInput.toString(), false,GlobalAttributeHandler.getSchemas()));
			if (jsonResponse.get("code").equals("1")) {
				return msgObject.put("message", "Success").toString();
			} else {
				return msgObject.put("message", jsonResponse.getString("message")).toString();
			}
		} catch (Exception e) {
			LOGGER.error(Thread.currentThread().getStackTrace()[0].getMethodName(), e);
			return "Something went wrong";
		}
	}

}
