package com.eit.abcdframework.serverbo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.httpclient.HttpStatus;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.eit.abcdframework.service.FormdataServiceImpl;
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

	@Autowired
	static FormdataServiceImpl formdataServiceImpl;

	private static final String KEY = "primarykey";
	private static final String REFLEX = "reflex";
	private static final String SUCCESS = "Success";
	private static final String ERROR = "error";

	private static final Logger LOGGER = LoggerFactory.getLogger("ResponcesHandling");

	private static JSONObject email = null;

	private static JSONObject getPushNotificationJsonObject = DisplaySingleton.memoryApplicationSetting
			.has("notificationConfig")
					? new JSONObject(DisplaySingleton.memoryApplicationSetting.get("notificationConfig").toString())
							.getJSONObject("sendnotification")
					: new JSONObject();

	public static String curdMethodResponceHandle(String response, JSONObject jsonbody, JSONObject jsonheader,
			JSONObject gettabledata,String method) {
		try {
			if (response.startsWith("{")) {
				jsonbody.put(gettabledata.getJSONObject(KEY).getString("columnname"),
						new JSONObject(response.toString())
								.get(gettabledata.getJSONObject(KEY).getString("columnname")));
				handlerMethod(jsonheader, jsonbody, gettabledata,method);

			} else if (response.equalsIgnoreCase("success")) {
				handlerMethod(jsonheader, jsonbody, gettabledata,method);
			} else if (Integer.parseInt(response) >= 200 && Integer.parseInt(response) <= 226) {
				handlerMethod(jsonheader, jsonbody, gettabledata,method);
			} else {
				String res = HttpStatus.getStatusText(Integer.parseInt(response));
				return new JSONObject().put(ERROR, res).toString();
			}
		} catch (Exception e) {
			LOGGER.error(Thread.currentThread().getStackTrace()[0].getMethodName(), e);
		}
		return new JSONObject().put(REFLEX, SUCCESS).toString();

	}

	private static void handlerMethod(JSONObject jsonheader, JSONObject jsonbody, JSONObject gettabledata,
			String method) {
		try {
			String rolename = jsonheader.has("rolename") ? jsonheader.getString("rolename") : "";
			String message = jsonheader.has("message") ? jsonheader.getString("message") : "";
			String status = jsonheader.has("status") ? jsonheader.getString("status") : "";

			boolean notification = jsonheader.has("notification") ? jsonheader.getBoolean("notification") : false;
			if (jsonheader.has("sms")) {
				String sms = commonServices.smsService(jsonbody, gettabledata, jsonheader.getString("sms"));
				LOGGER.warn("SMS -->{}", sms);
			}

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
								jsonheader.has("lang") ? jsonheader.getString("lang") : "en",method);
					}
				} catch (Exception e) {
					LOGGER.error("Throw Email Failure! -->:: {}", e.getMessage());
				}
			}
		} catch (Exception e) {
			LOGGER.error(Thread.currentThread().getStackTrace()[0].getMethodName(), e);
		}

	}

	public static String MappedCurdOperation(JSONObject getdataObject, String data) {
		String res = "";
		try {
			Map<String, Boolean> formDataResponces = new HashMap<>();
			JSONArray methods = getdataObject.getJSONObject("synchronizedCurdOperation").getJSONArray("Methods");
			JSONArray bodJson = getdataObject.getJSONObject("synchronizedCurdOperation").getJSONArray("bodyJson");
			JSONObject body = new JSONObject(data);
			for (int method = 0; method < methods.length(); method++) {
				String keyofmethods = methods.get(method).toString();
				if (keyofmethods.equalsIgnoreCase("post")) {
					res = formdataServiceImpl
							.transmittingDatapost(body.getJSONObject(bodJson.get(method).toString()).toString());
					formDataResponces.put(bodJson.get(method).toString(),
							(new JSONObject(res).has("reflex") ? true : false));
				} else if (keyofmethods.equalsIgnoreCase("put")) {
					res = formdataServiceImpl
							.transmittingDataput(body.getJSONObject(bodJson.get(method).toString()).toString());
					formDataResponces.put(bodJson.get(method).toString(),
							(new JSONObject(res).has("reflex") ? true : false));
				} else {

				}

				Set<String> Failed = formDataResponces.entrySet().stream().filter(entry -> !entry.getValue())
						.map(Map.Entry::getKey).collect(Collectors.toSet());
				res = Failed.isEmpty() ? "Success" : "Missed Api" + Failed;
				LOGGER.error(Thread.currentThread().getStackTrace()[0].getMethodName() + "-->{}", res);

			}

		} catch (Exception e) {
			LOGGER.error(Thread.currentThread().getStackTrace()[0].getMethodName(), e);
		}

		return res;
	}

}
