package com.eit.abcdframework.service;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

import org.apache.commons.httpclient.HttpStatus;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.eit.abcdframework.http.caller.Httpclientcaller;
import com.eit.abcdframework.serverbo.CommonServices;
import com.eit.abcdframework.serverbo.DisplaySingleton;
import com.eit.abcdframework.util.AmazonSMTPMail;
import com.eit.abcdframework.util.MessageServices;
import com.eit.abcdframework.util.TimeZoneServices;
import com.eit.abcdframework.websocket.WebSocketService;

@Service
public class FormdataServiceImpl implements FormdataService {

	@Autowired
	CommonServices commonServices;
	@Autowired
	Httpclientcaller dataTransmit;

	@Autowired
	AmazonSMTPMail amazonSMTPMail;

	@Autowired
	WebSocketService socketService;

	@Autowired
	PasswordEncoder encoder;

	private static final Logger LOGGER = LoggerFactory.getLogger("DCDesignDataServiceImpl");
	private static final String KEY = "primarykey";
	private static final String REFLEX = "reflex";
	private static final String SUCCESS = "Success";
	private static final String ERROR = "error";
	private static final String FAILURE = "Failure";
	private static final String DATAVALUE = "datavalue";

	@Value("${applicationurl}")
	private String pgrest;

	@Override
	public String transmittingDataget(String name, String primary, String where) {

		JSONObject displayConfig;
		String res = "";
		try {
			if (!name.matches("[a-zA-Z/_]+")) {
				name = CommonServices.decrypt(name);
				if (!primary.equalsIgnoreCase(""))
					primary = CommonServices.decrypt(primary);
				if (!where.equalsIgnoreCase(""))
					where = CommonServices.decrypt(where);
			}
			displayConfig = DisplaySingleton.memoryDispObjs2.getJSONObject(name);
			JSONObject gettabledata = new JSONObject(displayConfig.get("datas").toString());
			String columnprimarykey = gettabledata.getJSONObject(KEY).getString("columnname");
			res = transmittingDatapgrestget(columnprimarykey, pgrest, "GET", gettabledata, primary, where);
		} catch (Exception e) {
			LOGGER.error(Thread.currentThread().getStackTrace()[0].getMethodName(), e);
		}
		return res;
	}

	public String transmittingDatapost(String data) {
		JSONObject displayConfig;
		String res = "";
		JSONObject jsonObject1 = null;
		try {
			JSONObject jsonObjectdata = new JSONObject(data);
			if (jsonObjectdata.has("data"))
				jsonObject1 = new JSONObject(CommonServices.decrypt(jsonObjectdata.getString("data")));
			else
				jsonObject1 = jsonObjectdata;
			JSONObject jsonheader = new JSONObject(jsonObject1.getJSONObject("header").toString());
			boolean function = jsonheader.has("function") ? jsonheader.getBoolean("function") : false;
			String displayAlias = jsonheader.getString("name");
			JSONObject jsonbody = new JSONObject(jsonObject1.getJSONObject("body").toString());
			displayConfig = DisplaySingleton.memoryDispObjs2.getJSONObject(displayAlias);
			JSONObject gettabledata = new JSONObject(displayConfig.get("datas").toString());
			res = transmittingDatatopgrestpost(pgrest, "POST", gettabledata, jsonbody, function, jsonheader);
		} catch (Exception e) {
			LOGGER.error(Thread.currentThread().getStackTrace()[0].getMethodName(), e);
		}
		return res;
	}

	public String transmittingDataput(String data) {
		JSONObject displayConfig;
		String res = "";
		JSONObject jsonObject1 = null;
		try {
			JSONObject jsonObjectdata = new JSONObject(data);
			if (jsonObjectdata.has("data"))
				jsonObject1 = new JSONObject(CommonServices.decrypt(jsonObjectdata.getString("data")));
			else
				jsonObject1 = jsonObjectdata;
			JSONObject jsonheader = new JSONObject(jsonObject1.getJSONObject("header").toString());
			String displayAlias = jsonheader.getString("name");
			JSONObject jsonbody = new JSONObject(jsonObject1.getJSONObject("body").toString());
			displayConfig = DisplaySingleton.memoryDispObjs2.getJSONObject(displayAlias);
			JSONObject gettabledata = new JSONObject(displayConfig.get("datas").toString());
			String columnprimarykey = gettabledata.getJSONObject(KEY).getString("columnname");
			res = transmittingDatatopgrestput(columnprimarykey, pgrest, gettabledata, jsonbody, jsonheader);
		} catch (Exception e) {
			LOGGER.error(Thread.currentThread().getStackTrace()[0].getMethodName(), e);
		}
		return res;
	}

	@Override
	public String transmittingDataDel(String name, String primary, String where, boolean isdeleteall) {
		JSONObject displayConfig;
		String res = "";
		try {
			if (name.length() >= 30) {
				name = CommonServices.decrypt(name);
				if (!primary.equalsIgnoreCase(""))
					primary = CommonServices.decrypt(primary);
				if (!where.equalsIgnoreCase(""))
					where = CommonServices.decrypt(where);
			}
			displayConfig = DisplaySingleton.memoryDispObjs2.getJSONObject(name);
			JSONObject gettabledata = new JSONObject(displayConfig.get("datas").toString());
			String columnprimarykey = gettabledata.getJSONObject(KEY).getString("columnname");
			res = transmittingDatapgrestDel(columnprimarykey, pgrest, "Delete", gettabledata, primary, where,
					isdeleteall);
		} catch (Exception e) {
			LOGGER.error(Thread.currentThread().getStackTrace()[0].getMethodName(), e);
		}
		return res;
	}

	public String transmittingDatatopgrestpost(String url, String method, JSONObject gettabledata, JSONObject jsonbody,
			boolean function, JSONObject jsonheader) {
		JSONObject returndata = new JSONObject();
		String response = "";
		String res = "";
		JSONObject email = null;
		JSONArray mail = null;
		try {
			String rolename = jsonheader.has("rolename") ? jsonheader.getString("rolename") : "";
			String message = jsonheader.has("message") ? jsonheader.getString("message") : "";
			String status = jsonheader.has("status") ? jsonheader.getString("status") : "";
			boolean notification = jsonheader.has("notification") ? jsonheader.getBoolean("notification") : false;
//			boolean isProcess = gettabledata.has("storedProcess") ? gettabledata.getBoolean("storedProcess") : false;
			if (gettabledata.has("passwordencode") && gettabledata.getBoolean("passwordencode")) {
				if (jsonbody.has(gettabledata.getString("passwordcolumn"))
						&& !jsonbody.getString(gettabledata.getString("passwordcolumn")).equals("")) {
					jsonbody.put(gettabledata.getString("passwordcolumn"),
							encoder.encode(jsonbody.getString("user_password")));
				}
			}
			if (gettabledata.has("dateandtime")) {
				jsonbody.put(gettabledata.getString("dateandtime"),
						TimeZoneServices.getDateInTimeZoneforSKT("Asia/Riyadh"));
			}
			url = url + gettabledata.getString(method.toUpperCase());
			url = url.replace(" ", "%20");
			response = dataTransmit.transmitDataspgrestpost(url, jsonbody.toString(),
					jsonheader.has("primaryvalue") ? jsonheader.getBoolean("primaryvalue") : false);

			if (response.startsWith("{")) {
				if (jsonheader.has("sms")) {
					String sms = commonServices.smsService(jsonbody, gettabledata, jsonheader.getString("sms"));
					LOGGER.warn("SMS -->{}", sms);
				}
				jsonbody.put(gettabledata.getJSONObject(KEY).getString("columnname"),
						new JSONObject(response.toString())
								.get(gettabledata.getJSONObject(KEY).getString("columnname")));

				commonServices.sendPushNotification(jsonbody, gettabledata.getString("api"), rolename);

				String socketRes = socketService.pushSocketData(jsonheader, jsonbody, "");
				if (!socketRes.equalsIgnoreCase("Success")) {
					LOGGER.error("Push Socket responce::{}", socketRes);
				}
				if (gettabledata.has("activityLogs")) {
					String resp = "";
					if (!message.equalsIgnoreCase("") && !status.equalsIgnoreCase("")) {
						resp = commonServices.addactivitylog(gettabledata.getJSONObject("activityLogs"), status,
								jsonbody, rolename, message, notification);
					}
					LOGGER.error("ActivityLogs-->:: {}", resp);
					return returndata.put(REFLEX, SUCCESS).toString();
				}
				if (gettabledata.has("email")) {
					email = new JSONObject(gettabledata.get("email").toString());
//					if (email.has("MultiMail") && email.getBoolean("MultiMail")) {
//						String data = jsonbody.getString(email.getString("getcolumn")).equalsIgnoreCase("Approved")
//								? "Approved"
//								: jsonbody.getString(email.getString("getcolumn")).split(" ")[0]
//										.equalsIgnoreCase("Rejected") ? "Rejected" : "";
//
//						mail = new JSONObject(email.get("mail").toString()).has(data)
//								? new JSONObject(email.get("mail").toString()).getJSONArray(data)
//								: new JSONArray();
//
//					} else {
//						mail = new JSONArray(email.getJSONArray("mail").toString());
//					}
					if (!new JSONArray(email.getJSONObject("mail").toString()).isEmpty()) {
						List<MultipartFile> files = new ArrayList<>();
						amazonSMTPMail.emailconfig(email, jsonbody, files,jsonheader.has("lang") ? jsonheader.getString("lang") : "en");
					}
				}
			}

			if (response.equalsIgnoreCase("success")) {
				if (jsonheader.has("sms")) {
					String sms = commonServices.smsService(jsonbody, gettabledata, jsonheader.getString("sms"));
					LOGGER.warn("SMS -->{}", sms);
				}
				commonServices.sendPushNotification(jsonbody, gettabledata.getString("api"), rolename);
				String socketRes = socketService.pushSocketData(jsonheader, jsonbody, "");
				if (!socketRes.equalsIgnoreCase("Success")) {
					LOGGER.error("Push Socket responce::{}", socketRes);
				}
				if (gettabledata.has("activityLogs")) {
					String resp = "";
					if (!message.equalsIgnoreCase("") && !status.equalsIgnoreCase("")) {
						resp = commonServices.addactivitylog(gettabledata.getJSONObject("activityLogs"), status,
								jsonbody, rolename, message, notification);
					}
					LOGGER.error("ActivityLogs-->:: {}", resp);
					return returndata.put(REFLEX, SUCCESS).toString();
				}
				if (gettabledata.has("email")) {
					email = new JSONObject(gettabledata.get("email").toString());
//					if (email.has("MultiMail") && email.getBoolean("MultiMail")) {
//						String data = jsonbody.getString(email.getString("getcolumn")).equalsIgnoreCase("Approved")
//								? "Approved"
//								: jsonbody.getString(email.getString("getcolumn")).split(" ")[0]
//										.equalsIgnoreCase("Rejected") ? "Rejected" : "";
//
//						mail = new JSONObject(email.get("mail").toString()).has(data)
//								? new JSONObject(email.get("mail").toString()).getJSONArray(data)
//								: new JSONArray();
//
//					} else {
//						mail = new JSONArray(email.getJSONArray("mail").toString());
//					}
					if (!new JSONArray(email.getJSONObject("mail").toString()).isEmpty()) {
						List<MultipartFile> files = new ArrayList<>();
						amazonSMTPMail.emailconfig( email, jsonbody, files,jsonheader.has("lang") ? jsonheader.getString("lang") : "en");
					}
				}
			} else if (Integer.parseInt(response) >= 200 && Integer.parseInt(response) <= 226) {
				if (jsonheader.has("sms")) {
					String sms = commonServices.smsService(jsonbody, gettabledata, jsonheader.getString("sms"));
					LOGGER.warn("SMS -->{}", sms);
				}
				commonServices.sendPushNotification(jsonbody, gettabledata.getString("api"), rolename);

				String socketRes = socketService.pushSocketData(jsonheader, jsonbody, "");
				if (!socketRes.equalsIgnoreCase("Success")) {
					LOGGER.error("Respones of Data Pusing::{}", socketRes);
				}

				if (gettabledata.has("activityLogs")) {
					String resp = "";
					if (!message.equalsIgnoreCase("") && !status.equalsIgnoreCase("")) {
						resp = commonServices.addactivitylog(gettabledata.getJSONObject("activityLogs"), status,
								jsonbody, rolename, message, notification);
					}
					LOGGER.error("ActivityLogs-->:: {}", resp);
					return returndata.put(REFLEX, SUCCESS).toString();
				}
				if (gettabledata.has("email")) {
					email = new JSONObject(gettabledata.get("email").toString());
//					if (email.has("MultiMail") && email.getBoolean("MultiMail")) {
//						String data = jsonbody.getString(email.getString("getcolumn")).equalsIgnoreCase("Approved")
//								? "Approved"
//								: jsonbody.getString(email.getString("getcolumn")).split(" ")[0]
//										.equalsIgnoreCase("Rejected") ? "Rejected" : "";
//
//						mail = new JSONObject(email.get("mail").toString()).has(data)
//								? new JSONObject(email.get("mail").toString()).getJSONArray(data)
//								: new JSONArray();
//
//					} else {
//						mail = new JSONArray(email.getJSONArray("mail").toString());
//					}
					if (!new JSONArray(email.getJSONObject("mail").toString()).isEmpty()) {
						List<MultipartFile> files = new ArrayList<>();
						amazonSMTPMail.emailconfig( email, jsonbody, files,jsonheader.has("lang") ? jsonheader.getString("lang") : "en");
					}
				}

			} else {
				res = HttpStatus.getStatusText(Integer.parseInt(response));
				return new JSONObject().put(ERROR, res).toString();
			}

		} catch (

		Exception e) {
			LOGGER.error("Exception at {} ", Thread.currentThread().getStackTrace()[1].getMethodName(), e);
			return new JSONObject().put(ERROR, FAILURE).toString();
		}
		returndata.put(REFLEX, SUCCESS);
		return returndata.toString();
	}

	public String transmittingDatatopgrestput(String primarykey, String url, JSONObject gettabledata,
			JSONObject jsonbody, JSONObject jsonheader) {
		JSONObject returndata = new JSONObject();
		String response = "";
		String res = "";
		JSONObject email = null;
		JSONArray mail = null;
		try {
			String rolename = jsonheader.has("rolename") ? jsonheader.getString("rolename") : "";
			String message = jsonheader.has("message") ? jsonheader.getString("message") : "";
			String status = jsonheader.has("status") ? jsonheader.getString("status") : "";
			boolean notification = jsonheader.has("notification") ? jsonheader.getBoolean("notification") : false;
//			boolean isProcess = gettabledata.has("storedProcess") ? gettabledata.getBoolean("storedProcess") : true;

			if (gettabledata.has("passwordencode") && gettabledata.getBoolean("passwordencode")) {
				if (jsonbody.has(gettabledata.getString("passwordcolumn"))
						&& !jsonbody.getString(gettabledata.getString("passwordcolumn")).equals("")) {
					jsonbody.put(gettabledata.getString("passwordcolumn"),
							encoder.encode(jsonbody.getString("user_password")));
				}
			}

			if (jsonbody.has(primarykey) && !jsonbody.get(primarykey).toString().equalsIgnoreCase("")) {
				url = url + gettabledata.getString("api") + "?" + primarykey + "=eq." + jsonbody.get(primarykey);
				url = url.replace(" ", "%20");

				response = dataTransmit.transmitDataspgrestput(url, jsonbody.toString(),
						jsonheader.has("primaryvalue") ? jsonheader.getBoolean("primaryvalue") : false);
			} else {
				returndata.put(ERROR, "PrimaryKey is Missing!!");
			}
			if (Integer.parseInt(response) >= 200 && Integer.parseInt(response) <= 226) {
				if (jsonheader.has("sms")) {
					String sms = commonServices.smsService(jsonbody, gettabledata, jsonheader.getString("sms"));
					LOGGER.warn("SMS -->{}", sms);
				}
				String socketRes = socketService.pushSocketData(jsonheader, jsonbody, "");
				if (!socketRes.equalsIgnoreCase("Success")) {
					LOGGER.error("Push Socket responce::{}", socketRes);
				}
				if (gettabledata.has("email")) {
					email = new JSONObject(gettabledata.get("email").toString());
//					if (email.has("MultiMail") && email.getBoolean("MultiMail")) {
//						String data = jsonbody.getString(email.getString("getcolumn")).equalsIgnoreCase("Approved")
//								? "Approved"
//								: jsonbody.getString(email.getString("getcolumn")).split(" ")[0]
//										.equalsIgnoreCase("Rejected") ? "Rejected" : "";
//
//						mail = new JSONObject(email.get("mail").toString()).has(data)
//								? new JSONObject(email.get("mail").toString()).getJSONArray(data)
//								: new JSONArray();
//
//					} else {
//						mail = new JSONArray(email.getJSONArray("mail").toString());
//					}
					if (!email.getJSONObject("mail").toString().isEmpty()) {
						List<MultipartFile> files = new ArrayList<>();
						amazonSMTPMail.emailconfig(email, jsonbody, files,jsonheader.has("lang") ? jsonheader.getString("lang") : "en");
					}
				}
				if (gettabledata.has("activityLogs")) {
					String resp = "";
					if (!message.equalsIgnoreCase("") && !status.equalsIgnoreCase("")) {
						resp = commonServices.addactivitylog(gettabledata.getJSONObject("activityLogs"), status,
								jsonbody, rolename, message, notification);
					}
					LOGGER.error("ActivityLogs-->:: {}", resp);
					return returndata.put(REFLEX, SUCCESS).toString();
				}
			} else {
				res = HttpStatus.getStatusText(Integer.parseInt(response));
				return new JSONObject().put(ERROR, res).toString();
			}
		} catch (Exception e) {
			LOGGER.error("Exception at = {}", Thread.currentThread().getStackTrace()[1].getMethodName(), e);
			return new JSONObject().put(ERROR, FAILURE).toString();
		}

		return returndata.put(REFLEX, SUCCESS).toString();
	}

	public String transmittingDatapgrestget(String columnprimarykey, String url, String method, JSONObject gettabledata,
			String primary, String where) {
		JSONObject returndata = new JSONObject();
		JSONArray temparay;
		try {
			String regex = DisplaySingleton.memoryApplicationSetting.getString("UrlEncodeExcept");
			StringBuilder result = new StringBuilder();
			for (int i = 0; i < where.length(); i++) {
				char c = where.charAt(i);
				if (String.valueOf(c).matches(regex)) {
					// URL encode the special character
					String encodedChar = URLEncoder.encode(String.valueOf(c), "UTF-8");
					result.append(encodedChar);
				} else {
					result.append(c);
				}
			}
			System.err.println(result);

			String which = gettabledata.has("method") ? gettabledata.getString("method") : "GET";

			if (primary != null && !primary.equalsIgnoreCase("")) {

				url = url + gettabledata.getString(method.toUpperCase()) + "?" + columnprimarykey + "=eq." + primary;

			} else if (primary != null && primary.equalsIgnoreCase("") && !where.equalsIgnoreCase("")) {
				url = url + gettabledata.getString(method.toUpperCase()) + result;
			} else {
				url = url + gettabledata.getString("GET");
			}
			if (which.equalsIgnoreCase("post")) {
				temparay = new JSONArray();
				JSONObject json = null;
				JSONObject getdata = null;
				if (url.split("\\?").length > 1) {
					String data = url.split("\\?")[1];
					json = new JSONObject();
					String[] arraydata = data.split("&");
					for (int i = 0; i < arraydata.length; i++) {
						System.out.print(arraydata[i]);
						json.put(arraydata[i].split("=")[0], arraydata[i].split("=")[1]);
					}
					url = url.split("\\?")[0];
				} else {
					json = new JSONObject();
				}
				url = url.replace(" ", "%20");
				getdata = new JSONObject(dataTransmit.transmitDataspgrestpost(url, json.toString(), false));
				returndata.put(DATAVALUE, temparay.put(getdata));
			} else {
				url = url.replace(" ", "%20");
				temparay = dataTransmit.transmitDataspgrest(url);
				returndata.put(DATAVALUE, temparay);
				returndata.put(DATAVALUE, temparay);
			}

		} catch (Exception e) {
			returndata.put(ERROR, FAILURE);
			LOGGER.error("Exception at {}", Thread.currentThread().getStackTrace()[1].getMethodName(), e);
		}
		return returndata.toString();
	}

	public String transmittingDatapgrestDel(String columnprimarykey, String url, String method, JSONObject gettabledata,
			String primary, String where, boolean isdeleteall) {
		JSONObject returndata = new JSONObject();
		JSONArray temparay = null;
		int response = 0;
		String res = "";
		try {
			if (primary != null && !primary.equalsIgnoreCase("")) {

				url = url + gettabledata.getString(method.toUpperCase()) + "?" + columnprimarykey + "=eq." + primary;
				url = url.replace(" ", "%20");

				response = dataTransmit.transmitDataspgrestDel(url);
				returndata.put(DATAVALUE, temparay);
			} else if (primary != null && primary.equalsIgnoreCase("") && !where.equalsIgnoreCase("")) {
				url = url + gettabledata.getString(method.toUpperCase()) + where;
				url = url.replace(" ", "%20");

				response = dataTransmit.transmitDataspgrestDel(url);
			} else if (isdeleteall) {
				url = url + gettabledata.getString("api");
				response = dataTransmit.transmitDataspgrestDel(url);
			} else {
				return returndata.put(ERROR, "Please check the data").toString();
			}

			if (response >= 200 && response <= 226) {
				returndata.put(REFLEX, SUCCESS);
			} else {
				res = HttpStatus.getStatusText(response);
				returndata.put(ERROR, res);
			}

		} catch (Exception e) {
			returndata.put(ERROR, FAILURE);
			LOGGER.error("Exception at {}", Thread.currentThread().getStackTrace()[1].getMethodName(), e);
		}
		return returndata.toString();
	}

}