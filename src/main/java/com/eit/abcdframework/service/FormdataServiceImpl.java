package com.eit.abcdframework.service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import org.apache.commons.httpclient.HttpStatus;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.eit.abcdframework.globalhandler.GlobalAttributeHandler;
import com.eit.abcdframework.http.caller.Httpclientcaller;
import com.eit.abcdframework.serverbo.CommonServices;
import com.eit.abcdframework.serverbo.DisplaySingleton;
import com.eit.abcdframework.serverbo.ResponcesHandling;
import com.eit.abcdframework.util.AmazonSMTPMail;
import com.eit.abcdframework.util.TimeZoneServices;
import com.eit.abcdframework.websocket.WebSocketService;

@Service
public class FormdataServiceImpl implements FormdataService {

	@Autowired
	Httpclientcaller dataTransmit;

	@Autowired
	AmazonSMTPMail amazonSMTPMail;

	@Autowired
	WebSocketService socketService;

	@Autowired
	PasswordEncoder encoder;

	@Autowired
	ResponcesHandling responcesHandling;

	private static final Logger LOGGER = LoggerFactory.getLogger("DCDesignDataServiceImpl");

	@Override
	public String transmittingToMethod(String method, String data, String which) {
		JSONObject displayConfig;
		JSONObject jsonObject1 = null;
		String res = "";
		String councurrentAPIres = "";

		try {
			JSONObject jsonheader = null;
			JSONObject jsonbody = null;
			JSONObject bodyData = null;
			boolean synapi = false;

			JSONObject jsonObjectdata = new JSONObject(data);
			if (jsonObjectdata.has("data"))
				jsonObject1 = new JSONObject(CommonServices.decrypt(jsonObjectdata.getString("data")));
			else
				jsonObject1 = jsonObjectdata;

			if (which.equalsIgnoreCase("")) {

				synapi = jsonObject1.has("PrimaryBody") ? true : false;

				jsonheader = jsonObject1.has("PrimaryBody")
						? new JSONObject(jsonObject1.getJSONObject("PrimaryBody").getJSONObject("header").toString())
						: new JSONObject(jsonObject1.getJSONObject("header").toString());
				jsonbody = jsonObject1.has("PrimaryBody")
						? new JSONObject(jsonObject1.getJSONObject("PrimaryBody").getJSONObject("body").toString())
						: new JSONObject(jsonObject1.getJSONObject("body").toString());
			} else {
				synapi = jsonObject1.has("PrimaryBody") ? true : false;
				jsonheader = jsonObject1.has(which)
						? new JSONObject(jsonObject1.getJSONObject(which).getJSONObject("header").toString())
						: new JSONObject(jsonObject1.getJSONObject("header").toString());
				jsonbody = jsonObject1.has(which)
						? new JSONObject(jsonObject1.getJSONObject(which).getJSONObject("body").toString())
						: new JSONObject(jsonObject1.getJSONObject("body").toString());

			}

			String displayAlias = jsonheader.getString("name");
			displayConfig = DisplaySingleton.memoryDispObjs2.getJSONObject(displayAlias);
			JSONObject gettabledata = new JSONObject(displayConfig.get("datas").toString());

			// Check user has Valid or Not
			if (gettabledata.has("checkAPI")) {
				JSONObject checkingFunc = new JSONObject(gettabledata.get("checkAPI").toString());

				if (checkingFunc.getJSONArray("curd").toList().contains(method)) {
					String valueOF = new JSONObject(
							dataTransmit.transmitDataspgrest(GlobalAttributeHandler.getPgrestURL()
									+ checkingFunc.getString("FindTable") + jsonheader.get("UniqueColumn"),
									gettabledata.getString("schema")).get(0).toString())
							.getString("FetchColumn");

					if (valueOF.equalsIgnoreCase("MatchBy")) {
						return new JSONObject()
								.put(GlobalAttributeHandler.getError(), checkingFunc.getString("Message")).toString();
					}
				}

			}

			// Map to body Json
			jsonbody = mappingJson(gettabledata, jsonbody);

			boolean function = jsonheader.has("function") ? jsonheader.getBoolean("function") : false;

			if (gettabledata.has("expectedColumn")) {
				bodyData = new JSONObject(jsonbody.toString());
				jsonbody.remove(gettabledata.getString("expectedColumn"));

			} else {
				bodyData = new JSONObject(jsonbody.toString());
			}

			if (method.equalsIgnoreCase("POST")) {
				res = transmittingDatatopgrestpost(gettabledata, jsonbody, function, jsonheader);
			} else {
				res = transmittingDatatopgrestput(gettabledata, jsonbody, function, jsonheader);

			}

			if (res.equalsIgnoreCase(GlobalAttributeHandler.getFailure())) {
				return new JSONObject().put(GlobalAttributeHandler.getError(), GlobalAttributeHandler.getFailure())
						.toString();
			}

			responcesHandling.curdMethodResponceHandle(res, bodyData, jsonheader, gettabledata, method,
					new ArrayList<>());

			if (gettabledata.has("synchronizedCurdOperation") && synapi) {
				JSONArray typeOfMehods = gettabledata.getJSONObject("synchronizedCurdOperation").getJSONArray("type");
				for (int typeOfMehod = 0; typeOfMehod < typeOfMehods.length(); typeOfMehod++) {
					if (typeOfMehods.get(typeOfMehod).toString().equalsIgnoreCase("Map")) {
						councurrentAPIres = CommonServices.mappedCurdOperation(gettabledata, data);
						LOGGER.info("Councurrent API Response----->{}", councurrentAPIres);
					}
				}

			}

			if (councurrentAPIres.equalsIgnoreCase("Success")) {
				return new JSONObject().put(GlobalAttributeHandler.getReflex(), GlobalAttributeHandler.getSuccess())
						.toString();
			}

		} catch (Exception e) {
			LOGGER.error(Thread.currentThread().getStackTrace()[0].getMethodName(), e);
		}

		return new JSONObject().put(GlobalAttributeHandler.getReflex(), GlobalAttributeHandler.getSuccess()).toString();
	}

	private JSONObject mappingJson(JSONObject gettabledata, JSONObject jsonbody) {

		if (gettabledata.has("passwordencode") && gettabledata.getBoolean("passwordencode")) {
			if (jsonbody.has(gettabledata.getString("passwordcolumn"))
					&& !jsonbody.getString(gettabledata.getString("passwordcolumn")).equals("")) {
				jsonbody.put(gettabledata.getString("passwordcolumn"),
						encoder.encode(jsonbody.getString(gettabledata.getString("passwordcolumn"))));
			}
		}

		if (gettabledata.has("dateandtime")) {
			jsonbody.put(gettabledata.getString("dateandtime"),
					TimeZoneServices.getDateInTimeZoneforSKT("Asia/Riyadh"));
		}

		return jsonbody;

	}

	private String transmittingDatatopgrestpost(JSONObject gettabledata, JSONObject jsonbody, boolean function,
			JSONObject jsonheader) {
		String response = "";
		try {

			String url = (GlobalAttributeHandler.getPgrestURL() + gettabledata.getString("POST")).replaceAll(" ",
					"%20");
			response = dataTransmit.transmitDataspgrestpost(url, jsonbody.toString(),
					jsonheader.has("primaryvalue") ? jsonheader.getBoolean("primaryvalue") : false,
					gettabledata.getString("schema"));

		} catch (

		Exception e) {
			LOGGER.error("Exception at {} ", Thread.currentThread().getStackTrace()[1].getMethodName(), e);
			return GlobalAttributeHandler.getFailure();
		}
		return response;
	}

	private String transmittingDatatopgrestput(JSONObject gettabledata, JSONObject jsonbody, boolean function,
			JSONObject jsonheader) {
		String response = "";
		try {
			String primarykey = gettabledata.getJSONObject(GlobalAttributeHandler.getKey())
					.getString(GlobalAttributeHandler.getPrimarycolumnkey());

			if (jsonbody.has(primarykey) && !jsonbody.get(primarykey).toString().equalsIgnoreCase("")) {
				String url = GlobalAttributeHandler.getPgrestURL() + gettabledata.getString("PUT") + "?" + primarykey
						+ "=eq." + jsonbody.get(primarykey);
				url = url.replace(" ", "%20");

				response = dataTransmit.transmitDataspgrestput(url, jsonbody.toString(),
						jsonheader.has("primaryvalue") ? jsonheader.getBoolean("primaryvalue") : false,
						gettabledata.getString("schema"));
			} else {
				response = "PrimaryKey is Missing!!";
			}
		} catch (Exception e) {
			LOGGER.error("Exception at {} ", Thread.currentThread().getStackTrace()[1].getMethodName(), e);
			return GlobalAttributeHandler.getFailure();
		}
		return response;
	}

	@Override
	public String transmittingToMethod(String method, String name, String primary, String where, boolean isdeleteall) {
		String res = "";
		JSONObject displayConfig;
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
			String columnprimarykey = gettabledata.getJSONObject(GlobalAttributeHandler.getKey())
					.getString(GlobalAttributeHandler.getPrimarycolumnkey());

			if (method.equalsIgnoreCase("GET")) {
				res = transmittingDatapgrestget(columnprimarykey, method, gettabledata, primary, where);
			} else {
				res=transmittingDatapgrestDel(columnprimarykey, method, gettabledata, primary, where, isdeleteall);
			}

		} catch (Exception e) {
			LOGGER.error("Exception at {} ", Thread.currentThread().getStackTrace()[1].getMethodName(), e);
		}

		return res;

	}

	private String transmittingDatapgrestget(String columnprimarykey, String method, JSONObject gettabledata,
			String primary, String where) {
		System.out.println("Where : "+where);
		System.out.println("columnprimarykey : "+columnprimarykey);
		System.out.println("method : "+method);
		System.out.println("primary : "+primary);
		System.out.println("table data : "+gettabledata.toString());
		
		
		
		JSONObject returndata = new JSONObject();
		JSONArray temparay;
		try {
			String url = "";
			String which = gettabledata.has("method") ? gettabledata.getString("method") : "GET";
			LOGGER.info("method:" + method);
			if (gettabledata.getString(method).startsWith("rpc") && gettabledata.getBoolean("preDefined")) {
				JSONObject quryJson = gettabledata.getJSONObject("Query");
				LOGGER.info("quryjson: " + quryJson);
				if (quryJson.has("where")) {
					String whereCon = quryJson.getString("where")
							+ (where.equalsIgnoreCase("") ? "" : " and " + where.replace("?datas=", ""));
					quryJson.put("where", whereCon);
				} else if (!where.equalsIgnoreCase(""))
					quryJson.put("where", " where "+where.replace("?datas=", ""));

				url = GlobalAttributeHandler.getPgrestURL() + gettabledata.getString("Function") + "?basequery="
						+ gettabledata.getJSONObject("Query");
				System.err.println(url);
			} else if (primary != null && !primary.equalsIgnoreCase("")) {
				url = GlobalAttributeHandler.getPgrestURL() + gettabledata.getString(method.toUpperCase()) + "?"
						+ columnprimarykey + "=eq." + primary;
			} else if (primary != null && primary.equalsIgnoreCase("") && !where.equalsIgnoreCase("")) {
				url = GlobalAttributeHandler.getPgrestURL() + gettabledata.getString(method.toUpperCase()) + where;
			} else {
				url = GlobalAttributeHandler.getPgrestURL() + gettabledata.getString("GET");
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
				LOGGER.info(" if url: " + url);
				getdata = new JSONObject(new JSONArray(dataTransmit.transmitDataspgrestpost(url, json.toString(), false,
						gettabledata.getString("schema"))).get(0).toString());
				returndata.put(GlobalAttributeHandler.getDatavalue(), temparay.put(getdata));
			} else {
				LOGGER.info(" else url: " + url);
				temparay = dataTransmit.transmitDataspgrest(url, gettabledata.getString("schema"));
				returndata.put(GlobalAttributeHandler.getDatavalue(), temparay);
			}

		} catch (Exception e) {
			LOGGER.error("Exception at {}", Thread.currentThread().getStackTrace()[1].getMethodName(), e);
			return new JSONObject().put(GlobalAttributeHandler.getError(), GlobalAttributeHandler.getFailure())
					.toString();
		}
		return returndata.toString();
	}

	private String transmittingDatapgrestDel(String columnprimarykey, String method, JSONObject gettabledata,
			String primary, String where, boolean isdeleteall) {
		JSONObject returndata = new JSONObject();
		String response = "";
		try {
			String url = "";
			if (primary != null && !primary.equalsIgnoreCase("")) {
				url = (GlobalAttributeHandler.getPgrestURL() + gettabledata.getString(method.toUpperCase()) + "?"
						+ columnprimarykey + "=eq." + primary);
			} else if (primary != null && primary.equalsIgnoreCase("") && !where.equalsIgnoreCase("")) {
				url = (GlobalAttributeHandler.getPgrestURL() + gettabledata.getString(method.toUpperCase()) + where)
						;
				;

			} else if (isdeleteall) {
				url = url + gettabledata.getString("api");
			} else {
				return returndata.put(GlobalAttributeHandler.getError(), "Please check the data").toString();
			}

			response = dataTransmit.transmitDataspgrestDel(url, gettabledata.getString("schema"));

//			if (response >= 200 && response <= 226) {
//				returndata.put(GlobalAttributeHandler.getReflex(), GlobalAttributeHandler.getSuccess());
//			} else {
//				res = HttpStatus.getStatusText(response);
//				returndata.put(GlobalAttributeHandler.getError(), res);
//			}

		} catch (Exception e) {
			returndata.put(GlobalAttributeHandler.getError(), GlobalAttributeHandler.getFailure());
			LOGGER.error("Exception at {}", Thread.currentThread().getStackTrace()[1].getMethodName(), e);
		}
		return response;
	}

//	private StringBuilder URLEncode(String value) {
//		StringBuilder result = new StringBuilder();
//		try {
//			String regex = DisplaySingleton.memoryApplicationSetting.getString("UrlEncodeExcept");
//			for (int i = 0; i < value.length(); i++) {
//				char c = value.charAt(i);
////				System.err.println(c);
//				if (!isArabic(c)) {
//					if (String.valueOf(c).matches(regex)) {
//						// URL encode the special character
//						String encodedChar = URLEncoder.encode(String.valueOf(c), StandardCharsets.UTF_8.toString());
//						result.append(encodedChar);
//					} else {
//						result.append(c);
//					}
//				} else {
//					result.append(c);
//				}
//			}
//		} catch (Exception e) {
//			LOGGER.error("Exception at {}", Thread.currentThread().getStackTrace()[1].getMethodName(), e);
//		}
//		return result;
//	}
//
//	private static boolean isArabic(char c) {
//		return (c >= '\u0600' && c <= '\u06FF');
//	}

}