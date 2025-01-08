package com.eit.abcdframework.serverbo;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.apache.commons.httpclient.HttpStatus;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.eit.abcdframework.globalhandler.GlobalAttributeHandler;
import com.eit.abcdframework.http.caller.Httpclientcaller;
import com.eit.abcdframework.util.AmazonSMTPMail;
import com.eit.abcdframework.util.MessageServices;
import com.eit.abcdframework.util.TimeZoneServices;
import com.eit.abcdframework.websocket.WebSocketService;

@Service
public class ResponcesHandling {

	static WebSocketService socketService;

	static AmazonSMTPMail amazonSMTPMail;

	@Autowired
	public void setProductService(AmazonSMTPMail amazonSMTPMail) {
		ResponcesHandling.amazonSMTPMail = amazonSMTPMail;
	}

	@Autowired
	public void setProductService(WebSocketService socketService) {
		ResponcesHandling.socketService = socketService;
	}

	static Httpclientcaller dataTransmit;

	@Autowired
	public void setProductService(Httpclientcaller dataTransmit) {
		ResponcesHandling.dataTransmit = dataTransmit;
	}

	private static final Logger LOGGER = LoggerFactory.getLogger("ResponcesHandling");

	private static JSONObject email = null;

	@Async
	public CompletableFuture<String> curdMethodResponceHandle(String response, JSONObject jsonbody,
			JSONObject jsonheader, JSONObject gettabledata, String method, List<File> files) {
		try {

			if (response.startsWith("{")) {
				jsonbody.put(gettabledata.getJSONObject(GlobalAttributeHandler.getKey()).getString("columnname"),
						new JSONObject(response.toString()).get(
								gettabledata.getJSONObject(GlobalAttributeHandler.getKey()).getString("columnname")));
				handlerMethod(jsonheader, jsonbody, gettabledata, method, files);

			} else if (response.equalsIgnoreCase("success")) {
				handlerMethod(jsonheader, jsonbody, gettabledata, method, files);
//			} else if (Integer.parseInt(response) >= 200 && Integer.parseInt(response) <= 226) {
			} else if (new JSONObject((new JSONArray(response).get(0).toString())).has("reflex")) {
				handlerMethod(jsonheader, jsonbody, gettabledata, method, files);
			} else {
				String res = HttpStatus.getStatusText(Integer.parseInt(response));
				return CompletableFuture
						.completedFuture(new JSONObject().put(GlobalAttributeHandler.getError(), res).toString());
			}
		} catch (Exception e) {
			LOGGER.error(Thread.currentThread().getStackTrace()[0].getMethodName(), e);
		}
		return CompletableFuture.completedFuture(new JSONObject()
				.put(GlobalAttributeHandler.getReflex(), GlobalAttributeHandler.getSuccess()).toString());

	}

	private static void handlerMethod(JSONObject jsonheader, JSONObject jsonbody, JSONObject gettabledata,
			String method, List<File> files) {
		try {
			String rolename = jsonheader.has("rolename") ? jsonheader.getString("rolename") : "";
			String message = jsonheader.has("message") ? jsonheader.getString("message") : "";
			String status = jsonheader.has("status") ? jsonheader.getString("status") : "";

			boolean notification = jsonheader.has("notification") ? jsonheader.getBoolean("notification") : false;

			String socketRes = socketService.pushSocketData(jsonheader, jsonbody, "");
			if (socketRes != null && !socketRes.equalsIgnoreCase("Success")) {
				LOGGER.error("Push Socket responce::{}", socketRes);
			}
			if (gettabledata.has("email")) {
				try {
					email = new JSONObject(gettabledata.get("email").toString());
					if (!new JSONObject(email.get("mail").toString()).isEmpty()) {
						amazonSMTPMail.emailconfig(email, jsonbody, files,
								jsonheader.has("lang") ? jsonheader.getString("lang") : "en", method,
								gettabledata.getString("schema"));
					}
				} catch (Exception e) {
					LOGGER.error("Throw Email Failure! -->:: {}", e.getMessage());
				}
			}

			if (jsonheader.has("sms")) {
				String sms = smsService(jsonbody, gettabledata, jsonheader.getString("sms"));
				LOGGER.warn("SMS -->{}", sms);
			}

			if (!GlobalAttributeHandler.getNotificationConfig().isEmpty()
					&& GlobalAttributeHandler.getNotificationConfig().getJSONArray("tablename").toList()
							.contains(gettabledata.getString("api"))) {
				sendPushNotification(jsonbody, gettabledata.getString("api"), rolename,
						GlobalAttributeHandler.getNotificationConfig(), gettabledata.getString("schema"));
			}

			if (gettabledata.has("activityLogs")) {
				try {
					String resp = "";
					if (!message.equalsIgnoreCase("") && !status.equalsIgnoreCase("")) {
						resp = addactivitylog(gettabledata.getJSONObject("activityLogs"), status, jsonbody, rolename,
								message, notification, gettabledata.getString("schema"));
						LOGGER.error("ActivityLogs-->:: {}", resp);
					}

				} catch (Exception e) {
					LOGGER.error("ActivityLogs Failure!,Check it api -->:: {}", e.getMessage());
				}
			}

		} catch (Exception e) {
			LOGGER.error(Thread.currentThread().getStackTrace()[0].getMethodName(), e);
		}

	}

	private static String addactivitylog(JSONObject getvalue, String status, JSONObject jsonbody, String rolename,
			String message, boolean notification, String schema) {
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
			String url = GlobalAttributeHandler.getPgrestURL() + "activitylog";
			String response = dataTransmit.transmitDataspgrestpost(url, setvalue.toString(), false, schema);
//			if (Integer.parseInt(response) >= 200 && Integer.parseInt(response) <= 226) {
			if (new JSONObject((new JSONArray(response).get(0).toString())).has("reflex")) {
				if (notification) {
					sendPushNotification(setvalue, "activitylog", rolename,
							GlobalAttributeHandler.getNotificationConfig(), schema);
				}
				JSONObject header = new JSONObject();
				header.put("name", "activitylogs");
				header.put("rolename", rolename);
				socketService.pushSocketData(header, setvalue, "");
				returndata = "Success";
			}

		} catch (

		Exception e) {
			returndata = e.getMessage();
			LOGGER.error("Exception in addactivitylog : ", e);
		}
		return returndata;
	}

	private static String smsService(JSONObject jsonbody, JSONObject getdata, String msg) {
		JSONObject datavalue = null;
		JSONObject smsObject = null;
		try {
			smsObject = new JSONObject(getdata.get("sms").toString());
			if (smsObject.has("fetchby") && !smsObject.getJSONObject("fetchby").isEmpty()) {
				smsObject.getJSONObject("fetchby").getString("tablename");
				smsObject.getJSONObject("fetchby").getJSONArray("param");
				smsObject.getJSONObject("fetchby").getJSONArray("value");
				String url = GlobalAttributeHandler.getPgrestURL()
						+ smsObject.getJSONObject("fetchby").getString("tablename") + "?"
						+ smsObject.getJSONObject("fetchby").getJSONArray("param").get(0) + "=eq."
						+ jsonbody.get(smsObject.getJSONObject("fetchby").getJSONArray("value").get(0).toString());
				datavalue = new JSONObject(
						dataTransmit.transmitDataspgrest(url, getdata.getString("schema")).get(0).toString());

			}
		} catch (Exception e) {
			LOGGER.error("Exception at " + Thread.currentThread().getStackTrace()[1].getMethodName(), e);
			return "Failed";
		}
		return MessageServices
				.MsegatsmsService(datavalue.get(smsObject.getJSONObject("fetchby").getString("getby")).toString(), msg);
	}

	public static String sendPushNotification(JSONObject jsonbody, String tablename, String rolename,
			JSONObject getPushNotificationJsonObject, String schema) {
		String res = "success";

		try {
			String sendingdata = getPushNotificationJsonObject.getJSONObject(tablename).getString("sendingdata");
			String Findcolumn = getPushNotificationJsonObject.getJSONObject(tablename).getString("Findcolumn");
			if (!sendingdata.equalsIgnoreCase("all")) {
				if (jsonbody.getString(Findcolumn).equalsIgnoreCase(sendingdata)) {
					if (getPushNotificationJsonObject.getBoolean("sendbyrole")
							&& getPushNotificationJsonObject.getJSONArray("rolename").toList().contains(rolename))
						sendnotification(jsonbody, tablename, getPushNotificationJsonObject, schema);
					else if (!getPushNotificationJsonObject.getBoolean("sendbyrole"))
						sendnotification(jsonbody, tablename, getPushNotificationJsonObject, schema);
				}
			} else {
				if (getPushNotificationJsonObject.getBoolean("sendbyrole")
						&& getPushNotificationJsonObject.getJSONArray("rolename").toList().contains(rolename))
					sendnotification(jsonbody, tablename, getPushNotificationJsonObject, schema);
				else if (!getPushNotificationJsonObject.getBoolean("sendbyrole"))
					sendnotification(jsonbody, tablename, getPushNotificationJsonObject, schema);
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
					String url = (GlobalAttributeHandler.getPgrestURL() + api + "?" + where).replaceAll(" ", "%20");

					String toogleData = new JSONObject(dataTransmit.transmitDataspgrest(url, schema).get(0).toString())
							.getString(emailObject.getJSONObject("FindToggle").getString("columnkey"));
					if (toogleData.equalsIgnoreCase("On")) {

						if (!rolename.equalsIgnoreCase("Company Admin")) {
							jsonbody.put("from", DisplaySingleton.memoryApplicationSetting.getString("AdminName"));
							jsonbody.put("to", jsonbody.getString("companyname"));

							emailObject.put("mailid", false);
							emailObject.put("table", jsonbody.getString("displaytab").toLowerCase());

						} else {
							jsonbody.put("userid", DisplaySingleton.memoryApplicationSetting.getString("AdminMail"));
							jsonbody.put("to", DisplaySingleton.memoryApplicationSetting.getString("AdminName"));
							jsonbody.put("from",
									jsonbody.getString("companyname") + "-" + jsonbody.getString("username"));
						}
						amazonSMTPMail.emailconfig(emailObject, jsonbody, new ArrayList<>(), "en", "POST", schema);
						jsonbody.remove("from");
						jsonbody.remove("to");

					}

				}

			}

		} catch (Exception e) {
			LOGGER.error("Exception at " + Thread.currentThread().getStackTrace()[1].getMethodName(), e);
			return "Failed";
		}
		LOGGER.info(res);
		return res;
	}

	private static String sendnotification(JSONObject jsonBody, String tablename, JSONObject getJsonObject,
			String schema) {
		try {

			String url = GlobalAttributeHandler.getPgrestURL() + getJsonObject.getString("getToken")
					+ "?status=eq.login";
			JSONArray jsonArray = dataTransmit.transmitDataspgrest(url, schema);
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

			for (int i = 0; i < jsonArray.length(); i++) {
				if (!new JSONObject(jsonArray.get(i).toString()).getString("status").equalsIgnoreCase("login")) {
					if (jsonBody.has("appnotification") && jsonBody.getBoolean("appnotification")) {
						setvalueofnotification.put("token",
								new JSONObject(jsonArray.get(i).toString()).getString("pushnotificationtoken"));
						dataTransmit.transmitDataPushNotification(getJsonObject.getString("api"),
								new JSONObject().put("message", setvalueofnotification).toString());
					}
				} else {
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
					}
				}
			}

		} catch (

		Exception e) {
			LOGGER.error("Exception at Send Notification::", e);
		}
		return "success";

	}

	@Async
	public CompletableFuture<String> asyncProcess(String response) {
		// Here you can process the response asynchronously
		try {
			// Simulate some processing logic
			Thread.sleep(2000);
			System.out.println("Processing response asynchronously: " + response);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
		return CompletableFuture.completedFuture("Processing completed");
	}

}
