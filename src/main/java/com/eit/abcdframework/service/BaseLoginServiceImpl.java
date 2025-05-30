package com.eit.abcdframework.service;

import org.apache.commons.httpclient.HttpStatus;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;

import com.eit.abcdframework.http.caller.Httpclientcaller;
import com.eit.abcdframework.serverbo.CommonServices;
import com.eit.abcdframework.serverbo.DisplaySingleton;
import com.eit.abcdframework.util.TimeZoneServices;

@Service
public class BaseLoginServiceImpl implements BaseLoginService {
	@Autowired
	DisplaySingleton displaySingleton;

	@Autowired
	Httpclientcaller datatrans;

	@Autowired
	CommonServices commonServices;

	@Value("${applicationurl}")
	private String pgresturl;
	
	@Value("${schema}")
	private String schema;

	private static final Logger LOGGER = LoggerFactory.getLogger(BaseLoginServiceImpl.class);

	@Override
	public String authenticateUser(JSONArray user, String token) {
		JSONObject returnMessage = new JSONObject();
		String url = "";
		try {

			if (!(new JSONObject(user.get(0).toString()).getBoolean("mailverification"))) {
				return returnMessage.put("reflex", "Kindly click the link provided in the email!!").toString();
			} else {
				url = pgresturl + "rpc/generate_logsid";
				String primaryValue = new JSONObject(
						datatrans.transmitDataspgrestpost(url, new JSONObject().toString(), false,schema)).getString("value");
				returnMessage.put("companyid", new JSONObject(user.get(0).toString()).get("companyid"));
				returnMessage.put("username", new JSONObject(user.get(0).toString()).getString("emailaddress"));
				returnMessage.put("company", new JSONObject(user.get(0).toString()).getString("companyname"));
				returnMessage.put("token", token);
				returnMessage.put("mainmeu", new JSONObject(user.get(0).toString()).getString("mainmenu"));
				returnMessage.put("role", new JSONObject(user.get(0).toString()).getString("rolename"));
				returnMessage.put("isprimaryuser", new JSONObject(user.get(0).toString()).get("isprimaryuser"));
				returnMessage.put("logsValue", primaryValue);

				commonServices.userstatusupdate("login", new JSONObject(user.get(0).toString()), primaryValue);

			}
//			JSONObject formdata = displaySingleton.memoryApplicationForms;
//			returnMessage.put("forms", formdata.toString());

		} catch (

		BadCredentialsException ex) {
			LOGGER.error(" Exception :: getApplicationLoginData :: inValied Username and password ");
			return " Invalid Username or password ";
		} catch (Exception e) {
			LOGGER.error(" Exception :: getApplicationLoginData :: inValied Username and password ", e);
			return " Invalid Username or password ";
		}

		return returnMessage.toString();

	}

	@Override
	public String verification(String name, String email,String lang) {
		String returnMessages = "";
		try {
			returnMessages = commonServices.sendMailForVerification(email, "otp",lang);

		} catch (Exception e) {
			LOGGER.error("Exception at {}", Thread.currentThread().getStackTrace()[1].getMethodName(), e);
		}

		return returnMessages;
	}

	@Override
	public String pushNotificationUpdate(boolean token, JSONObject user, String id, String notifitoke,
			JSONObject notifiJson) {
		JSONObject returndata = new JSONObject();
		String url = "";
		String response = "";
		String tablename = "";
		try {
			String sendby = "";
			JSONObject json = new JSONObject();
			if (token) {
				String urls = pgresturl + "pushnotification?pushnotificationid=eq." + id + "&select=pushnotificationid";
				if (!datatrans.transmitDataspgrest(urls,schema).isEmpty()) {
					sendby = "put";
					tablename = "pushnotification?pushnotificationid=eq." + id;
				} else {
					sendby = "post";
					tablename = "pushnotification";
				}
				json.put("pushnotificationid", id);
				json.put("pushnotificationtoken", notifitoke);
				json.put("status", "login");
				json.put("logintime", TimeZoneServices.getDateInTimeZoneforSKT("Asia/Riyadh"));
				if (!user.isEmpty()) {
					JSONArray dataArray = notifiJson.getJSONObject("usesdata").getJSONArray("columnname");
					if (!dataArray.isEmpty()) {
						json.put("username", user.getString(dataArray.get(0).toString()));
						json.put("userid", user.get(dataArray.get(1).toString()));

					}
				}
			} else if (!token) {
				json.put("pushnotificationid", id);
				json.put("pushnotificationtoken", notifitoke);
				json.put("status", "logout");
				json.put("logouttime", TimeZoneServices.getDateInTimeZoneforSKT("Asia/Riyadh"));
				tablename = "pushnotification?pushnotificationid=eq." + id;
				sendby = "put";
			}

			url = pgresturl + tablename;
			if (sendby.equalsIgnoreCase("post"))
				response = datatrans.transmitDataspgrestpost(url, json.toString(), false,schema);
			else
				response = datatrans.transmitDataspgrestput(url, json.toString(), false,schema);

//			if (Integer.parseInt(response) <= 200 || Integer.parseInt(response) >= 226) {
			if(new JSONObject((new JSONArray(response).get(0).toString())).has("reflex")) {
				String res = HttpStatus.getStatusText(Integer.parseInt(response));
				returndata.put("error", res);
			} else {
				returndata.put("reflex", "Success");

			}
		} catch (Exception e) {
			LOGGER.error("Exception at {}", Thread.currentThread().getStackTrace()[1].getMethodName(), e);
		}
		return returndata.toString();
	}

}
