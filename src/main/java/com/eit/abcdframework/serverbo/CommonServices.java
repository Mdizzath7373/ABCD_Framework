package com.eit.abcdframework.serverbo;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.httpclient.HttpStatus;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.eit.abcdframework.http.caller.Httpclientcaller;
import com.eit.abcdframework.util.AmazonSMTPMail;
import com.eit.abcdframework.util.MessageServices;
import com.eit.abcdframework.util.TimeZoneServices;
import com.eit.abcdframework.websocket.WebSocketService;

@Service
public class CommonServices {

	@Value("${applicationurl}")
	private String applicationurl;

	@Autowired
	Httpclientcaller dataTransmit;

	@Autowired
	WebSocketService socketService;

	@Autowired
	AmazonSMTPMail amazonSMTPMail;

	@Autowired
	MessageServices messageServices;

	private static final Logger LOGGER = LoggerFactory.getLogger("CommonServices");

	// Define characters allowed in the verification code
	private static final String ALLOWED_CHARACTERS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";

	// Define the maximum length of the verification code
	private static final int MAX_CODE_LENGTH = 6;

	private static final String ALGORITHM = "AES/CBC/PKCS5Padding"; // AES with CBC and PKCS5 padding
	private static final String SECRET_KEY = "ABCDFRAM09876543"; // 16-byte key for AES
	private static final String IV = "ABCDFRAMIV098765"; // 16-byte IV for AES

	public String addactivitylog(JSONObject getvalue, String status, JSONObject jsonbody, String rolename,
			String message, boolean notification) {
		String returndata = "";
		try {

			JSONArray param = getvalue.getJSONArray("param");
			JSONObject setvalue = getvalue.getJSONObject("setvalue");
			setvalue.put("logs", message);
			setvalue.put("createdby", rolename);
			setvalue.put("activitytype", status);
			setvalue.put("createdtime", TimeZoneServices.getDateInTimeZoneforSKT("Asia/Riyadh"));
			for (int i = 0; i < param.length(); i++) {
				setvalue.put(param.getString(i),
						jsonbody.get(getvalue.getJSONObject("getvalues").get(param.get(i).toString()).toString()));
			}
			String url = applicationurl + "activitylog";
			String response = dataTransmit.transmitDataspgrestpost(url, setvalue.toString(), false);
			if (Integer.parseInt(response) >= 200 && Integer.parseInt(response) <= 226) {
				if (notification) {
					sendPushNotification(setvalue, "activitylog", rolename, new JSONObject());
				}
				JSONObject header = new JSONObject();
				header.put("name", "activitylogs");
				header.put("rolename", rolename);
				socketService.pushSocketData(header, setvalue, "");
				returndata = "Success";
			}

		} catch (Exception e) {
			returndata = e.getMessage();
			LOGGER.error("Exception in addactivitylog : ", e);
		}
		return returndata;
	}

	private String sendnotification(JSONObject jsonBody, String tablename, JSONObject getJsonObject) {
		try {

			String url = applicationurl + getJsonObject.getString("getToken") + "?status=eq.login";
			JSONArray jsonArray = dataTransmit.transmitDataspgrest(url);
			if (jsonArray.isEmpty()) {
				return "No token Found";
			}

			JSONObject setvalueofnotification = new JSONObject();
			JSONObject notificationbody = new JSONObject();
			if (getJsonObject.getJSONObject(tablename).getBoolean("setdata")) {
				JSONArray columnarray = getJsonObject.getJSONObject(tablename).getJSONArray("column");
				JSONArray getvalue = getJsonObject.getJSONObject(tablename).getJSONArray("getvalues");
				JSONObject dataJson = new JSONObject();
				for (int j = 0; j < columnarray.length(); j++) {
					dataJson.put(columnarray.get(j).toString(), jsonBody.get(getvalue.get(j).toString()).toString());
				}
				setvalueofnotification.put("data", dataJson);
			}

			JSONArray titlevalue = getJsonObject.getJSONObject(tablename).getJSONArray("title");

			if (titlevalue.length() > 1) {
				String data = "";
				for (int t = 0; t < titlevalue.length(); t++) {

					if (t + 1 == titlevalue.length())
						data = data + jsonBody.get(titlevalue.get(t).toString()).toString();
					else if (t != 0)
						data = data + jsonBody.get(titlevalue.get(t).toString()) + "-";
					else
						data = jsonBody.get(titlevalue.get(t).toString()) + "-";
				}

				setvalueofnotification.put("notification", notificationbody.put("title", data));
			} else {
				setvalueofnotification.put("notification",
						notificationbody.put("title", jsonBody.get(titlevalue.get(0).toString())));
			}
			JSONArray body = getJsonObject.getJSONObject(tablename).getJSONArray("body");
			if (body.length() > 1) {
				String data = "";
				for (int t = 0; t < body.length(); t++) {
					if (t + 1 == body.length())
						data = data + jsonBody.get(body.get(t).toString()).toString();
					else if (t != 0)
						data = data + jsonBody.get(body.get(t).toString()) + "-";
					else
						data = jsonBody.get(body.get(t).toString()) + "-";
				}

				setvalueofnotification.put("notification", notificationbody.put("body", data));

			} else {
				setvalueofnotification.put("notification",
						notificationbody.put("body", jsonBody.get(body.get(0).toString())));
			}
			// Onborad
//			setvalueofnotification.put("priority", "high");

			for (int i = 0; i < jsonArray.length(); i++) {
				if (!new JSONObject(jsonArray.get(i).toString()).getString("status").equalsIgnoreCase("login")) {
					if (jsonBody.has("appnotification") && jsonBody.getBoolean("appnotification")) {
						setvalueofnotification.put("token",
								new JSONObject(jsonArray.get(i).toString()).getString("pushnotificationtoken"));
						dataTransmit.transmitDataPushNotification(getJsonObject.getString("api"),
								new JSONObject().put("message", setvalueofnotification).toString());
					}
				} else {
//					System.err.println(jsonBody.get(getJsonObject.getJSONObject(tablename).getString("checkUser")));
//					System.err.println(new JSONObject(jsonArray.get(i).toString()).get("userid"));
					if (getJsonObject.getJSONObject(tablename).has("checkUser")
							&& jsonBody.get(getJsonObject.getJSONObject(tablename).getString("checkUser"))
									.equals(new JSONObject(jsonArray.get(i).toString()).get("userid"))) {
						setvalueofnotification.put("token",
								new JSONObject(jsonArray.get(i).toString()).getString("pushnotificationtoken"));
						dataTransmit.transmitDataPushNotification(getJsonObject.getString("api"),
								new JSONObject().put("message", setvalueofnotification).toString());
					} else if (!getJsonObject.getJSONObject(tablename).has("checkUser")) {
						setvalueofnotification.put("token",
								new JSONObject(jsonArray.get(i).toString()).getString("pushnotificationtoken"));
						dataTransmit.transmitDataPushNotification(getJsonObject.getString("api"),
								new JSONObject().put("message", setvalueofnotification).toString());
						// Onborad
//						setvalueofnotification.put("to",
//								new JSONObject(jsonArray.get(i).toString()).getString("pushnotificationtoken"));
//						dataTransmit.transmitDataPushNotification("https://fcm.googleapis.com/fcm/send",
//								setvalueofnotification.toString());
					}
				}
//				https://fcm.googleapis.com/v1/projects/refa-f55bd/messages:send
//					https://fcm.googleapis.com/fcm/send

			}
		} catch (

		Exception e) {
			LOGGER.error("Exception at Send Notification::", e);
		}
		return "success";

	}

	public String userstatusupdate(String updationform, JSONObject user, String id) {
		JSONObject returndata = new JSONObject();
		String url = "";
		String response = "";
		String tablename = "";
		try {
			String sendby = "";
			JSONObject json = new JSONObject();
			if (updationform.equalsIgnoreCase("login")) {
				json.put("id", id);
				json.put("companyid", user.get("companyid"));
				json.put("username", user.getString("emailaddress"));
				json.put("companyname", user.getString("companyname"));
				json.put("logintime", TimeZoneServices.getDateInTimeZoneforSKT("Asia/Riyadh"));
				tablename = "logsofuser";
				sendby = "post";
			} else if (updationform.equalsIgnoreCase("logout")) {
				json.put("id", id);
				json.put("logouttime", TimeZoneServices.getDateInTimeZoneforSKT("Asia/Riyadh"));
				tablename = "logsofuser?id=eq." + id;
				sendby = "put";
			}
			url = applicationurl + tablename;
			if (sendby.equalsIgnoreCase("post"))
				response = dataTransmit.transmitDataspgrestpost(url, json.toString(), false);
			else
				response = dataTransmit.transmitDataspgrestput(url, json.toString(), false);

			if (Integer.parseInt(response) <= 200 || Integer.parseInt(response) >= 226) {
				String res = HttpStatus.getStatusText(Integer.parseInt(response));
				returndata.put("error", res);
			} else {
				returndata.put("reflex", "Success");
			}

		} catch (Exception e) {
			LOGGER.error("Exception at User Status Update ::", e);
		}
		return returndata.toString();
	}

	public String userActivation(String key) {
		JSONObject returnMessage = new JSONObject();
		try {
			JSONObject getconfigofactivation = new JSONObject(
					DisplaySingleton.memoryApplicationSetting.get("useractivationConfig").toString());

			String url = applicationurl + getconfigofactivation.getString("tablename") + "?"
					+ getconfigofactivation.getString("verificationcolumn") + "=eq." + key;
			JSONArray userData = dataTransmit.transmitDataspgrest(url);
			if (!userData.isEmpty()) {
				JSONObject datas = new JSONObject(userData.get(0).toString());
				if (!datas.getBoolean("mailverification")) {
					datas.put("mailverification", true);
					url = applicationurl + getconfigofactivation.getString("tablename") + "?"
							+ getconfigofactivation.getString("primarykey") + "=eq."
							+ datas.get(getconfigofactivation.getString("primarykey"));
					String result = dataTransmit.transmitDataspgrestput(url, datas.toString(), false);
					if (Integer.parseInt(result) >= 200 && Integer.parseInt(result) <= 226) {
						returnMessage.put("reflex", "Successfully Verified");
					}
				} else {
					return returnMessage.put("error", "This User Already Verified").toString();
				}
			} else {
				return returnMessage.put("error", "Verification Failed").toString();
			}
		} catch (

		Exception e) {
			returnMessage.put("error", "Verification Failed");
			LOGGER.error("Exception at " + Thread.currentThread().getStackTrace()[1].getMethodName(), e);
		}
		return returnMessage.toString();
	}

	public static String generateVerificationCode(int maxLength) {
		SecureRandom random = new SecureRandom();
		StringBuilder codeBuilder = new StringBuilder(maxLength);

		// Generate random characters for the code
		for (int i = 0; i < maxLength; i++) {
			int randomIndex = random.nextInt(ALLOWED_CHARACTERS.length());
			char randomChar = ALLOWED_CHARACTERS.charAt(randomIndex);
			codeBuilder.append(randomChar);
		}

		return codeBuilder.toString();
	}

	public Long generateVerificationOTP(String id) {
		String ALLOWED_CHARACTERS = "0123456789";
		Long OTP = null;
//		Long OTP = (long) Math.floor(1000 + Math.random() * 9000);
		try {
			JSONObject forgotJson = new JSONObject(
					DisplaySingleton.memoryApplicationSetting.get("forgotpassConfig").toString());
			int otplen = forgotJson.getInt("otplength");

			SecureRandom random = new SecureRandom();
			StringBuilder codeBuilder = new StringBuilder(otplen);

			// Generate random characters for the code
			for (int i = 0; i < otplen; i++) {
				int randomIndex = random.nextInt(ALLOWED_CHARACTERS.length());
				char randomChar = ALLOWED_CHARACTERS.charAt(randomIndex);
				codeBuilder.append(randomChar);
			}
			OTP = Long.parseLong(codeBuilder.toString());
			if (forgotJson.getBoolean("OTPStore")) {
				JSONObject jsonbody = new JSONObject();
				jsonbody.put("user_id", id);
				jsonbody.put("otp_code", OTP);
				String url = applicationurl + "/otp_verification?user_id=eq." + id;
				JSONArray dataArray = dataTransmit.transmitDataspgrest(url);
				if (dataArray.isEmpty()) {
					url = applicationurl + "/otp_verification";
					dataTransmit.transmitDataspgrestpost(url, jsonbody.toString(), false);
				} else {
//					JSONObject jsonData=new JSONObject(dataArray.get(0).toString());
//					if(jsonData.getInt("attempts")<3) {
//						if(jsonData.getString("created_at").equalsIgnoreCase(new Date))
//					}
					jsonbody.put("attempts", 0);
					jsonbody.put("id", new JSONObject(dataArray.get(0).toString()).get("id"));
					url = applicationurl + "/otp_verification?id=eq."
							+ new JSONObject(dataArray.get(0).toString()).get("id");
					dataTransmit.transmitDataspgrestput(url, jsonbody.toString(), false);
				}
			}
		} catch (Exception e) {
			LOGGER.error("Exception at " + Thread.currentThread().getStackTrace()[1].getMethodName(), e);
		}
		return OTP;

	}

	public String VerificationOTP(String OTP, String id) {
		JSONObject jsonBody = null;
		String res = "";
		try {
			String url = applicationurl + "/otp_verification?user_id=eq." + id;
			JSONArray dataArray = dataTransmit.transmitDataspgrest(url);
			if (!dataArray.isEmpty()) {
				jsonBody = new JSONObject(dataArray.get(0).toString());
				if (jsonBody.getInt("attempts") < 3) {
					if (jsonBody.getString("otp_code").equalsIgnoreCase(OTP)) {
						jsonBody.put("attempts", jsonBody.getInt("attempts") + 1);
						jsonBody.put("verified", true);
						url = applicationurl + "/otp_verification?id=eq." + jsonBody.get("id");
						dataTransmit.transmitDataspgrestput(url, jsonBody.toString(), false);
						res = "Verified";
					} else {
						jsonBody.put("attempts", jsonBody.getInt("attempts") + 1);
						res = "Retry Verification OTP Dose not Match";
						url = applicationurl + "/otp_verification?id=eq." + jsonBody.get("id");
						dataTransmit.transmitDataspgrestput(url, jsonBody.toString(), false);
					}
				} else {
					return "Too Many Attempt,So Please Retry After 24Hrs";
				}

			}

		} catch (Exception e) {
			LOGGER.error("Exception at " + Thread.currentThread().getStackTrace()[1].getMethodName(), e);
		}

		return res;

	}

	public String sendMailForVerification(String mail, String mode, String lang) {
		JSONObject returndata = new JSONObject();
		String body = "";
		String subject = "";
		String code = "";
		try {
			if (mode.equalsIgnoreCase("link")) {

			} else if (mode.equalsIgnoreCase("otp")) {
				code = CommonServices.generateVerificationCode(MAX_CODE_LENGTH);
				body = new JSONObject(
						(new JSONObject(DisplaySingleton.memoryEmailCofig.get("otpverification").toString())
								.get("contenttype")).toString())
						.getString(lang).replaceAll("codeOTP", code);
			}
			subject = new JSONObject(DisplaySingleton.memoryEmailCofig.get("otpverification").toString())
					.getJSONObject("subject").getString(lang);
			amazonSMTPMail.sendEmail(mail, subject, body);

			returndata.put("reflex", Base64.getEncoder().encodeToString(code.getBytes()));
		} catch (Exception e) {
			LOGGER.error("Exception at " + Thread.currentThread().getStackTrace()[1].getMethodName(), e);
			returndata.put("error", "Faild Verification");
		}
		return returndata.toString();

	}

	public String storedProcess(JSONObject stroedProcess, String method, JSONObject bodyJson) {
		JSONObject storedData = new JSONObject();
		try {
			String getJson = method.equalsIgnoreCase("POST") ? "insert" : "update";

			JSONObject getStoredProcessData = stroedProcess.getJSONObject(getJson);
			for (int i = 0; i < getStoredProcessData.getJSONArray("tablename").length(); i++) {
				JSONObject dataOfJson = getStoredProcessData
						.getJSONObject(getStoredProcessData.getJSONArray("tablename").get(i).toString());
//              JSONObject values=new JSONObject();
				for (int j = 0; j < dataOfJson.getJSONArray("columnname").length(); j++) {
					storedData.put(dataOfJson.getJSONArray("columnname").get(j).toString() + "-" + getJson,
							bodyJson.getString(dataOfJson.getJSONArray("jsonname").get(j).toString()));
				}

			}
			dataTransmit.transmitDataspgrestpost(applicationurl + "rpc/storedFunction", storedData.toString(), false);

		} catch (Exception e) {
			// TODO: handle exception
		}
		return "";
	}

	public String sendPushNotification(JSONObject jsonbody, String tablename, String rolename,
			JSONObject getPushNotificationJsonObject) {
		String res = "success";

		try {
			String sendingdata = getPushNotificationJsonObject.getJSONObject(tablename).getString("sendingdata");
			String Findcolumn = getPushNotificationJsonObject.getJSONObject(tablename).getString("Findcolumn");
			if (!sendingdata.equalsIgnoreCase("all")) {
				if (jsonbody.getString(Findcolumn).equalsIgnoreCase(sendingdata)) {
					if (getPushNotificationJsonObject.getBoolean("sendbyrole")
							&& getPushNotificationJsonObject.getJSONArray("rolename").toList().contains(rolename))
						sendnotification(jsonbody, tablename, getPushNotificationJsonObject);
					else if (!getPushNotificationJsonObject.getBoolean("sendbyrole"))
						sendnotification(jsonbody, tablename, getPushNotificationJsonObject);
				}
			} else {
				if (getPushNotificationJsonObject.getBoolean("sendbyrole")
						&& getPushNotificationJsonObject.getJSONArray("rolename").toList().contains(rolename))
					sendnotification(jsonbody, tablename, getPushNotificationJsonObject);
				else if (!getPushNotificationJsonObject.getBoolean("sendbyrole"))
					sendnotification(jsonbody, tablename, getPushNotificationJsonObject);
			}
			System.err.println();

			if (getPushNotificationJsonObject.getJSONObject(tablename).has("email")) {
				JSONObject emailObject = new JSONObject(
						getPushNotificationJsonObject.getJSONObject(tablename).get("email").toString());

				if (emailObject.getBoolean("Toggle")) {
					String api = emailObject.getJSONObject("FindToggle").getString("tablename");
					String where = emailObject.getJSONObject("FindToggle").getJSONObject("where").getString("condition")
							+ jsonbody.get(
									emailObject.getJSONObject("FindToggle").getJSONObject("where").getString("value"));
					String url = (applicationurl + api + "?" + where).replaceAll(" ", "%20");

					String toogleData = new JSONObject(dataTransmit.transmitDataspgrest(url).get(0).toString())
							.getString(emailObject.getJSONObject("FindToggle").getString("columnkey"));
					if (toogleData.equalsIgnoreCase("On")) {

						if (!rolename.equalsIgnoreCase("Company Admin")) {
							jsonbody.put("from", DisplaySingleton.memoryApplicationSetting.getString("AdminName"));
							jsonbody.put("to",
									jsonbody.getString("companyname") + "-" + jsonbody.getString("username"));
						} else {
							jsonbody.put("to", DisplaySingleton.memoryApplicationSetting.getString("AdminName"));
							jsonbody.put("from",
									jsonbody.getString("companyname") + "-" + jsonbody.getString("username"));
						}
						amazonSMTPMail.emailconfig(emailObject, jsonbody, new ArrayList<>(), "en", "POST");
						jsonbody.remove("from");
						jsonbody.remove("to");

					}

				}

			}

		} catch (Exception e) {
			LOGGER.error("Exception at " + Thread.currentThread().getStackTrace()[1].getMethodName(), e.getMessage());
			return "Failed";
		}
		LOGGER.info(res);
		return res;
	}

	public String whereFormation(JSONObject jsonbody, JSONObject whereFormation) {
		String whereCondition = "";
		try {
			JSONArray arrayofwhere = whereFormation.getJSONArray("where");

			for (int i = 0; i < arrayofwhere.length(); i++) {
				if (i == 0) {
					whereCondition = "?" + arrayofwhere.get(i).toString() + "=eq.";
					String getby = whereFormation.getJSONArray("value").get(i).toString().split("-")[0];
					if (getby.equalsIgnoreCase("param")) {
						whereCondition += whereFormation.getJSONArray("value").get(i).toString().split("-")[1];
					} else if (getby.equalsIgnoreCase("body")) {
						System.err.println(whereFormation.getJSONArray("value").get(i).toString());
						whereCondition += jsonbody
								.get(whereFormation.getJSONArray("value").get(i).toString().split("-")[1]);

					}
				} else {
					whereCondition = "&" + arrayofwhere.get(i).toString() + "=eq.";
					String getby = whereFormation.getJSONArray("value").get(i).toString().split("-")[0];
					if (getby.equalsIgnoreCase("param")) {
						whereCondition += whereFormation.getJSONArray("value").get(i).toString().split("-")[1];
					} else if (getby.equalsIgnoreCase("body")) {
						whereCondition += jsonbody
								.get(whereFormation.getJSONArray("value").get(i).toString().split("-")[1]);
					}

				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return whereCondition;
	}

	public static String decrypt(String encryptedData) throws Exception {
		SecretKeySpec keySpec = new SecretKeySpec(SECRET_KEY.getBytes(), "AES");
		IvParameterSpec ivSpec = new IvParameterSpec(IV.getBytes());

		Cipher cipher = Cipher.getInstance(ALGORITHM);
		cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);

		byte[] decodedBytes = Base64.getDecoder().decode(encryptedData);
		byte[] decryptedBytes = cipher.doFinal(decodedBytes);

		return new String(decryptedBytes);
	}

	public String smsService(JSONObject jsonbody, JSONObject getdata, String msg) {
		JSONObject datavalue = null;
		JSONObject smsObject = null;
		try {
			smsObject = new JSONObject(getdata.get("sms").toString());
			if (smsObject.has("fetchby") && !smsObject.getJSONObject("fetchby").isEmpty()) {
				smsObject.getJSONObject("fetchby").getString("tablename");
				smsObject.getJSONObject("fetchby").getJSONArray("param");
				smsObject.getJSONObject("fetchby").getJSONArray("value");
				String url = applicationurl + smsObject.getJSONObject("fetchby").getString("tablename") + "?"
						+ smsObject.getJSONObject("fetchby").getJSONArray("param").get(0) + "=eq."
						+ jsonbody.get(smsObject.getJSONObject("fetchby").getJSONArray("value").get(0).toString());
				datavalue = new JSONObject(dataTransmit.transmitDataspgrest(url).get(0).toString());

			}
		} catch (Exception e) {
			LOGGER.error("Exception at " + Thread.currentThread().getStackTrace()[1].getMethodName(), e);
			return "Failed";
		}
		return messageServices
				.MsegatsmsService(datavalue.get(smsObject.getJSONObject("fetchby").getString("getby")).toString(), msg);
	}

	public Map<String, Object> loadBase64(String value, int total_pages) throws JSONException, IOException {
		String url = "";
		Map<String, Object> base64String = new HashMap<>();

		if (total_pages <= 100) {
			url = applicationurl + "pdf_splitter?select=document&primary_id_pdf=eq." + value;
			return new JSONObject(dataTransmit.transmitDataspgrest(url).get(0).toString()).getJSONObject("document")
					.toMap();

		} else if (total_pages > 100) {
			int start_page = 1;
			ExecutorService executorService = Executors.newFixedThreadPool(10);

			try {
				while (start_page < total_pages) {
					final int current_start_page = start_page;
					final int current_end_page = (start_page == 1 ? (start_page + 49) : (start_page + 50)) > total_pages
							? total_pages
							: start_page == 1 ? (start_page + 49) : (start_page + 50);

					executorService.submit(() -> {

						String urls = applicationurl + "rpc/get_pdf_splitdata?start_page=" + current_start_page
								+ "&end_page=" + current_end_page + "&datas=primary_id_pdf='" + value + "'";

						try {
							JSONObject jsonObject = new JSONObject(
									dataTransmit.transmitDataspgrest(urls).get(0).toString()).getJSONObject("images");

							jsonObject.keys().forEachRemaining(key -> {
								synchronized (base64String) {
									base64String.put(key, jsonObject.getString(key));
								}
							});

							LOGGER.info("Successfully Put Into Object startPages: {} --- endPage: {}",
									current_start_page, current_end_page);

						} catch (Exception e) {
							LOGGER.error("Error processing pages {} to {}: {}", current_start_page, current_end_page,
									e.getMessage());
						}
					});
					start_page = current_end_page > total_pages ? total_pages : current_end_page;
				}
			} finally {
				executorService.shutdown();
				try {
					if (!executorService.awaitTermination(60, TimeUnit.MINUTES)) {
						executorService.shutdownNow();
					}
				} catch (InterruptedException e) {
					executorService.shutdownNow();
					Thread.currentThread().interrupt();
				}
			}
		}
		System.err.println(base64String.size());
		return base64String;

	}

}
