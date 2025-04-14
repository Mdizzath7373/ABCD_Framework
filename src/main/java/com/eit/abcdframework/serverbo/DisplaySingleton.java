package com.eit.abcdframework.serverbo;

import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.eit.abcdframework.http.caller.Httpclientcaller;
import com.eit.abcdframework.util.TimeZoneServices;
import com.eit.abcdframework.websocket.WebSocketService;

import jakarta.annotation.PostConstruct;

@Component
public class DisplaySingleton {
	private static final Logger LOGGER = LoggerFactory.getLogger("DisplaySingleton");
	@Autowired
	Httpclientcaller dataTransmit;

	@Value("${applicationurl}")
	private String applicationurl;

	@Value("${schema}")
	private String schema;

	@Autowired
	TimeZoneServices timeZoneServices;

	@Autowired
	WebSocketService socket;

	public static final JSONObject memoryDispObjs2 = new JSONObject();
	public static final JSONObject memoryApplicationSetting = new JSONObject();
	public static final JSONObject memoryApplicationForms = new JSONObject();
	public static final JSONObject memoryEmailCofig = new JSONObject();

	public static final JSONObject addressCache = new JSONObject();

	private DisplaySingleton() {

	}

	@PostConstruct
	public void loadDisplayObjs() {

		try {
			configsObj();
			applictionsettingObj();
//			applictionformObj();
			emailConfigObj();

		} catch (Exception e) {
			LOGGER.error("Exception in loadDisplayObjs : ", e);
		}
	}

	public JSONObject configsObj() {
		try {
			String url = applicationurl + "configs";
			JSONArray getArrayObj = dataTransmit.transmitDataspgrest(url, schema);
			for (int i = 0; i < getArrayObj.length(); i++) {
				JSONObject json = new JSONObject(getArrayObj.get(i).toString());
				memoryDispObjs2.put(json.get("alias").toString(), getArrayObj.get(i));
			}
			String length = String.valueOf(memoryDispObjs2.length());

			LOGGER.info("size of display objects = {}", length);

		} catch (Exception e) {
			LOGGER.error("Exception in getconfigsObj : ", e);
		}
		return memoryDispObjs2;
	}

	public JSONObject applictionsettingObj() {
		try {
			String url = applicationurl + "applicationsetting";
			JSONArray getArrayObj = dataTransmit.transmitDataspgrest(url, schema);
			for (int i = 0; i < getArrayObj.length(); i++) {
				JSONObject json = new JSONObject(getArrayObj.get(i).toString());
				memoryApplicationSetting.put(json.getString("applicationkey"), json.getString("appvalue"));
			}
			String length = String.valueOf(memoryApplicationSetting.length());
			LOGGER.info("size of display objects = {}", length);

		} catch (Exception e) {
			LOGGER.error("Exception in getapplictionsetting : ", e);
		}
		return memoryApplicationSetting;
	}

//	public JSONObject applictionformObj() {
//		try {
//			String url = applicationurl + "applicationforms";
//			JSONArray getArrayObj = dataTransmit.transmitDataspgrest(url);
//			for (int i = 0; i < getArrayObj.length(); i++) {
//				JSONObject json = new JSONObject(getArrayObj.get(i).toString());
//				memoryApplicationForms.put(json.getString("applicationname"), new JSONArray(json.getString("forms")));
//			}
//			String length = String.valueOf(memoryApplicationForms.length());
//			LOGGER.info("size of display objects =  {}", length);
//
//		} catch (Exception e) {
//			LOGGER.error("Exception in getapplictionformObj : ", e);
//		}
//		return memoryApplicationForms;
//	}
	public JSONObject emailConfigObj() {
		try {
			String url = applicationurl + "emailconfig";
			JSONArray getArrayObj = dataTransmit.transmitDataspgrest(url, schema);
			for (int i = 0; i < getArrayObj.length(); i++) {
				JSONObject json = new JSONObject(getArrayObj.get(i).toString());
				memoryEmailCofig.put(
						json.getString("name")
								+ (!json.get("sentto").equals(null) ? "," + json.getString("sentto").equals(null) : ""),
						json);
			}
			String length = String.valueOf(memoryEmailCofig.length());
			LOGGER.info("size of display objects =  {}", length);

		} catch (Exception e) {
			LOGGER.error("Exception in getemailConfigObj : ", e);
		}
		return memoryEmailCofig;
	}

//	public String addactivitylog(JSONObject getactivityvalue, String method, JSONObject jsonbody, String rolename) {
//		String returndata = "";
//		try {
//			JSONObject getvalue = new JSONObject(getactivityvalue.get(method).toString());
//			JSONObject json = new JSONObject();
//			String logs = getvalue.getString("logs");
//			String prefix = getvalue.has("prefix") ? getvalue.getString("prefix") : "";
//			JSONArray sufix = new JSONArray(getvalue.getJSONArray("suffix").toString());
//			for (int i = 0; i < sufix.length(); i++) {
//				String data = sufix.get(i).toString();
//				System.err.println(jsonbody.get(data).toString());
//				if (i == 0)
//					logs = getvalue.getString("logs").replace("suffix" + i, jsonbody.get(data).toString());
//				else
//					logs = logs.replace("suffix" + i, jsonbody.get(data).toString());
//			}
//			if (getvalue.has("prefix")) {
//				logs = logs.replace("prefix", jsonbody.get(getvalue.getString("prefix")).toString());
//			}
//			json.put("logs", logs);
//			json.put("companyname", memorycompanydetails.get(jsonbody.get("ids").toString()));
//			json.put("ids", jsonbody.get("ids"));
//			json.put("data", jsonbody.get(getvalue.getString("primaryvalue")));
//			json.put("displaytype", getvalue.getString("displaytype"));
//			json.put("navigatedisplaytab", getvalue.getString("navigatedisplaytab"));
//			json.put("createdby", rolename);
//			json.put("activitytype", method);
//			json.put("createdtime", timeZoneServices.getDateInTimeZone(new Date()));
//			String url = applicationurl + "activitylog";
//			String response = dataTransmit.transmitDataspgrestpost(url, json.toString());
//			if (Integer.parseInt(response) >= 200 && Integer.parseInt(response) <= 226) {
//				JSONObject header = new JSONObject();
//				header.put("name", "activitylogs");
//				header.put("rolename", rolename);
//			socket.pushSocketData(header, json, "");
//				returndata = "Success";
//			}
//
//		} catch (Exception e) {
//			returndata = e.getMessage();
//			LOGGER.error("Exception in addactivitylog : ", e);
//		}
//		return returndata;
//	}
//
//	public String addactivitylogwithreason(JSONObject getactivityvalue, String method, JSONObject jsonbody,
//			String status, String reason, String rolename) {
//		String returndata = "";
//		try {
//			JSONObject getvalue = new JSONObject(getactivityvalue.get(method).toString());
//			JSONObject json = new JSONObject();
//			json.put("logs", reason);
//			json.put("companyname", memorycompanydetails.get(jsonbody.get("ids").toString()));
//			json.put("ids", jsonbody.get("ids"));
//			json.put("data", jsonbody.get(getvalue.getString("primaryvalue")));
//			json.put("displaytype", getvalue.getString("displaytype"));
//			json.put("navigatedisplaytab", getvalue.getString("navigatedisplaytab"));
//			json.put("createdby", rolename);
//			json.put("activitytype", status);
//			json.put("createdtime", timeZoneServices.getDateInTimeZone(new Date()));
//			String url = applicationurl + "activitylog";
//			String response = dataTransmit.transmitDataspgrestpost(url, json.toString());
//			if (Integer.parseInt(response) >= 200 && Integer.parseInt(response) <= 226) {
//				JSONObject header = new JSONObject();
//				header.put("name", "activitylogs");
//				header.put("rolename", rolename);
//				pushSocketData(header, jsonbody, "");
//				returndata = "Success";
//			}
//
//		} catch (Exception e) {
//			returndata = e.getMessage();
//			LOGGER.error("Exception in addactivitylogwithreason : ", e);
//		}
//		return returndata;
//	}

}