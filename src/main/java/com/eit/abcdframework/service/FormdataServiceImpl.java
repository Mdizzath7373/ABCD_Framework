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

			LOGGER.info("Fetching data from Config");
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
				LOGGER.info("Enter into POST Method process");
				res = transmittingDatatopgrestpost(gettabledata, jsonbody, function, jsonheader);
			} else {
				LOGGER.info("Enter into PUT Method process");
				res = transmittingDatatopgrestput(gettabledata, jsonbody, function, jsonheader);

			}

			if (res.equalsIgnoreCase(GlobalAttributeHandler.getFailure())) {
				LOGGER.error("Responce Failure :::{}", res);
				return new JSONObject().put(GlobalAttributeHandler.getError(), GlobalAttributeHandler.getFailure())
						.toString();
			}

			LOGGER.info("Success, Enter into Responce Handle Method");
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
	public String transmittingToMethod(String method, String name, String primary, String where, boolean isdeleteall, boolean isbulk) {
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
				res = transmittingDatapgrestget(columnprimarykey, method, gettabledata, primary, where, isbulk);
			} else {
				res = transmittingDatapgrestDel(columnprimarykey, method, gettabledata, primary, where, isdeleteall, isbulk);
			}
 
		} catch (Exception e) {
			LOGGER.error("Exception at {} ", Thread.currentThread().getStackTrace()[1].getMethodName(), e);
		}

		return res;

	}



	private String transmittingDatapgrestget(String columnprimarykey, String method, JSONObject gettabledata,
			String primary, String where, boolean isgetbulk) {
		JSONObject returndata = new JSONObject();
		JSONArray temparay;
		try {
			String url = "";
			String which = gettabledata.has("method") ? gettabledata.getString("method") : "GET";
			if (gettabledata.getString(method).startsWith("rpc") &&gettabledata.has("preDefined") && gettabledata.getBoolean("preDefined")) {
				JSONObject quryJson =null;
				String data="";
				if (gettabledata.getJSONObject("Query").has("where")) {
					 data=buildOrderedJsonString(gettabledata.getJSONObject("Query"), where);
				} else if (!where.equalsIgnoreCase("")) {
					data=buildOrderedJsonString(gettabledata.getJSONObject("Query"), where);
				}
				
				System.err.println(data);
				url = GlobalAttributeHandler.getPgrestURL() + gettabledata.getString("Function") + "?basequery="
						+ data;
				System.err.println(url);
			} else if (primary != null && !primary.equalsIgnoreCase("")) {
				if (isgetbulk)
					url = (GlobalAttributeHandler.getPgrestURL() + gettabledata.getString(method.toUpperCase()) + "?"
							+ columnprimarykey + "=in.(" + primary + ")");
				else
					url = (GlobalAttributeHandler.getPgrestURL() + gettabledata.getString(method.toUpperCase()) + "?"
							+ columnprimarykey + "=eq." + primary);
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
				getdata = new JSONObject(new JSONArray(dataTransmit.transmitDataspgrestpost(url, json.toString(), false,
						gettabledata.getString("schema"))).get(0).toString());
				returndata.put(GlobalAttributeHandler.getDatavalue(), temparay.put(getdata));
			} else {
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

	private static final String[] JSON_FIELDS_ORDER = { "query", "where", "groupby", "having", "orderby", "limit" };

// Method to build an ordered JSONObject from an existing JSONObject

public static String buildOrderedJsonString(JSONObject sourceJson, String where) {
    StringBuilder jsonBuilder = new StringBuilder();
    jsonBuilder.append("{");
    
    boolean isFirst = true;
    for (String field : JSON_FIELDS_ORDER) {
        if (sourceJson.has(field) || (field.equals("where") && !where.isEmpty())) {
            // Add comma if not the first field
            if (!isFirst) {
                jsonBuilder.append(",");
            }
            isFirst = false;
            
            // Handle the 'where' field specially
            if (field.equals("where")) {
                if (!where.isEmpty()) {
                	where=where.replace("?datas=","");
                    if (sourceJson.has(field)) {
                        // Both source JSON has where and we have additional where condition
                        jsonBuilder.append("  \"").append(field).append("\": ");
                        jsonBuilder.append("\"").append(sourceJson.optString(field).replace("\"", "\\\""))
                                .append(" and ").append(where.replace("\"", "\\\"")).append("\"");
                    } else {
                        // Only our where condition
                        jsonBuilder.append("  \"").append(field).append("\": ");
                        jsonBuilder.append("\" where ").append(where.replace("\"", "\\\"")).append("\"");
                    }
                } else if (sourceJson.has(field)) {
                    // Only source JSON has where
                    jsonBuilder.append("  \"").append(field).append("\": ");
                    jsonBuilder.append("\"").append(sourceJson.optString(field).replace("\"", "\\\"")).append("\"");
                }
            } else if (sourceJson.has(field)) {
                // Handle all other fields
                jsonBuilder.append("  \"").append(field).append("\": ");
                Object value = sourceJson.opt(field);
                if (value instanceof String) {
                    jsonBuilder.append("\"").append(((String) value).replace("\"", "\\\"")).append("\"");
                } else {
                    jsonBuilder.append(value);
                }
            }
        }
    }
    
    jsonBuilder.append("}");
    return jsonBuilder.toString();
}


	private String transmittingDatapgrestDel(String columnprimarykey, String method, JSONObject gettabledata,
			String primary, String where, boolean isdeleteall, boolean isdeletebulk) {
		JSONObject returndata = new JSONObject();
		String response = "";
		try {
			String url = "";
			if (primary != null && !primary.equalsIgnoreCase("")) {
				if(!isdeleteall&&isdeletebulk) 		
					url = (GlobalAttributeHandler.getPgrestURL() + gettabledata.getString(method.toUpperCase()) + "?"
							+ columnprimarykey + "=in.(" + primary +")");
				else
					url = (GlobalAttributeHandler.getPgrestURL() + gettabledata.getString(method.toUpperCase()) + "?"
							+ columnprimarykey + "=eq." + primary);
			} else if (primary != null && primary.equalsIgnoreCase("") && !where.equalsIgnoreCase("")) {
				url = (GlobalAttributeHandler.getPgrestURL() + gettabledata.getString(method.toUpperCase()) + where);
 
			} else if (isdeleteall) {
				url = url + gettabledata.getString("api");
			} else {
				return returndata.put(GlobalAttributeHandler.getError(), "Please check the data").toString();
			}
			
			response = dataTransmit.transmitDataspgrestDel(url, gettabledata.getString("schema"));

		} catch (Exception e) {
			returndata.put(GlobalAttributeHandler.getError(), GlobalAttributeHandler.getFailure());
			LOGGER.error("Exception at {}", Thread.currentThread().getStackTrace()[1].getMethodName(), e);
		}
		return response;
	}


	@Override
	public String transmittingToMethodBulk(String method, String data) {
		JSONObject displayConfig;
		JSONObject jsonObject1 = null;
		String res = "";

		try {
			JSONObject jsonheader = null;
			JSONArray jsonbody = null;

			JSONObject jsonObjectdata = new JSONObject(data);
			if (jsonObjectdata.has("data"))
				jsonObject1 = new JSONObject(CommonServices.decrypt(jsonObjectdata.getString("data")));
			else
				jsonObject1 = jsonObjectdata;

			jsonheader = new JSONObject(jsonObject1.getJSONObject("header").toString());
			jsonbody = new JSONArray(jsonObject1.getJSONArray("body").toString());

			String displayAlias = jsonheader.getString("name");
			displayConfig = DisplaySingleton.memoryDispObjs2.getJSONObject(displayAlias);
			JSONObject gettabledata = new JSONObject(displayConfig.get("datas").toString());

			if (method.equalsIgnoreCase("POST")) {
				res = transmittingDatatopgrestpostBulk(gettabledata, jsonbody, jsonheader);
			} else {
				res = transmittingDatatopgrestputBulk(gettabledata, jsonbody, jsonheader);

			}

			if (res.equalsIgnoreCase(GlobalAttributeHandler.getFailure())) {
				return new JSONObject().put(GlobalAttributeHandler.getError(), GlobalAttributeHandler.getFailure())
						.toString();
			}

		} catch (Exception e) {
			LOGGER.error(Thread.currentThread().getStackTrace()[0].getMethodName(), e);
		}

		return new JSONObject().put(GlobalAttributeHandler.getReflex(), GlobalAttributeHandler.getSuccess()).toString();
	}
	
	private String transmittingDatatopgrestputBulk(JSONObject gettabledata, JSONArray jsonbody,
	        JSONObject jsonheader) {
	    String response = "";
	    try {
	        String url = (GlobalAttributeHandler.getPgrestURL() + gettabledata.getString("PUT")+"?on_conflict="+gettabledata.getJSONObject("primarykey").getString("columnname")).replaceAll(" ", "%20");

	        String jsonBodyString = jsonbody.toString();

	        response = dataTransmit.transmitDataspgrestPutbulk(
	                url, 
	                jsonBodyString, 
	                jsonheader.has("primaryvalue") ? jsonheader.getBoolean("primaryvalue") : false,
	                gettabledata.getString("schema"));

	    } catch (Exception e) {
	        LOGGER.error("Exception at {} ", Thread.currentThread().getStackTrace()[1].getMethodName(), e);
	        return GlobalAttributeHandler.getFailure();
	    }
	    return response;
	}



	private String transmittingDatatopgrestpostBulk(JSONObject gettabledata, JSONArray jsonbody,
			JSONObject jsonheader) {
		String response = "";
		try {
			String url = (GlobalAttributeHandler.getPgrestURL() + gettabledata.getString("POST")).replaceAll(" ",
					"%20");

			String jsonBodyString = jsonbody.toString();

			response = dataTransmit.transmitDataspgrestpost(url, jsonBodyString,
					jsonheader.has("primaryvalue") ? jsonheader.getBoolean("primaryvalue") : false,
					gettabledata.getString("schema"));

		} catch (Exception e) {
			LOGGER.error("Exception at {} ", Thread.currentThread().getStackTrace()[1].getMethodName(), e);
			return GlobalAttributeHandler.getFailure();
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