package com.eit.abcdframework.serverbo;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.eit.abcdframework.util.AmazonSMTPMail;
import com.eit.abcdframework.websocket.WebSocketService;

@Service
public class ResponcesHandling {
	@Autowired
	static CommonServices commonServices;

	@Autowired
	static WebSocketService socketService;

	@Autowired
	static AmazonSMTPMail amazonSMTPMail;

	private static final String KEY = "primarykey";
	private static final String REFLEX = "reflex";
	private static final String SUCCESS = "Success";

	private static final Logger LOGGER = LoggerFactory.getLogger("ResponcesHandling");

	private static JSONObject email = null;

	private static JSONObject getPushNotificationJsonObject = DisplaySingleton.memoryApplicationSetting
			.has("notificationConfig")
					? new JSONObject(DisplaySingleton.memoryApplicationSetting.get("notificationConfig").toString())
							.getJSONObject("sendnotification")
					: new JSONObject();

	public static String curdMethodResponceHandle(String response, JSONObject jsonbody, JSONObject jsonheader,
			JSONObject gettabledata) {
		JSONObject returndata = new JSONObject();
		try {
			String rolename = jsonheader.has("rolename") ? jsonheader.getString("rolename") : "";
			String message = jsonheader.has("message") ? jsonheader.getString("message") : "";
			String status = jsonheader.has("status") ? jsonheader.getString("status") : "";
			boolean notification = jsonheader.has("notification") ? jsonheader.getBoolean("notification") : false;
			if (response.startsWith("{")) {
				if (jsonheader.has("sms")) {
					String sms = commonServices.smsService(jsonbody, gettabledata, jsonheader.getString("sms"));
					LOGGER.warn("SMS -->{}", sms);
				}

				jsonbody.put(gettabledata.getJSONObject(KEY).getString("columnname"),
						new JSONObject(response.toString())
								.get(gettabledata.getJSONObject(KEY).getString("columnname")));

				if (!getPushNotificationJsonObject.isEmpty() && getPushNotificationJsonObject.getJSONArray("tablename")
						.toList().contains(gettabledata.getString("api"))) {
					commonServices.sendPushNotification(jsonbody, gettabledata.getString("api"), rolename,
							getPushNotificationJsonObject);
				}
				
				String socketRes = socketService.pushSocketData(jsonheader, jsonbody, "");
				if (!socketRes.equalsIgnoreCase("Success")) {
					LOGGER.error("Push Socket responce::{}", socketRes);
				}

				if (gettabledata.has("activityLogs")) {
					try {
						String resp = "";
						if (!message.equalsIgnoreCase("") && !status.equalsIgnoreCase("")) {
							resp = commonServices.addactivitylog(gettabledata.getJSONObject("activityLogs"), status,
									jsonbody, rolename, message, notification);
						}
						LOGGER.error("ActivityLogs-->:: {}", resp);
						return returndata.put(REFLEX, SUCCESS).toString();
					} catch (Exception e) {
						LOGGER.error("ActivityLogs Failure!,Check it api -->:: {}", e.getMessage());
					}
				}
				if (gettabledata.has("email")) {
					try {
						email = new JSONObject(gettabledata.get("email").toString());
						if (!new JSONArray(email.getJSONObject("mail").toString()).isEmpty()) {
							List<MultipartFile> files = new ArrayList<>();
							amazonSMTPMail.emailconfig(email, jsonbody, files,
									jsonheader.has("lang") ? jsonheader.getString("lang") : "en");
						}
					} catch (Exception e) {
						LOGGER.error("Throw Email Failure! -->:: {}", e.getMessage());
					}
				}
			}
		} catch (Exception e) {
			// TODO: handle exception
		}

		return "";

	}

}
