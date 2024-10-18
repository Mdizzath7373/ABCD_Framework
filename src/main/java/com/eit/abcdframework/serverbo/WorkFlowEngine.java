package com.eit.abcdframework.serverbo;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.eit.abcdframework.globalhandler.GlobalAttributeHandler;
import com.eit.abcdframework.http.caller.Httpclientcaller;
import com.eit.abcdframework.util.AmazonSMTPMail;

@Service
public class WorkFlowEngine {

	@Autowired
	Httpclientcaller dataTransmit;

	@Autowired
	DisplaySingleton displaySingleton;

	@Autowired
	PasswordEncoder encoder;

	@Autowired
	AmazonSMTPMail amazonSMTPMail;

	@Autowired
	FileuploadServices fileuploadServices;

	@Value("${FromNameOfMail}")
	private String FromNameOfMail;

//	@Value("${applicationurl}")
//	private String pgresturl;
	
//	@Value("${schema}")
//	private String schema;


	private static final Logger LOGGER = LoggerFactory.getLogger(WorkFlowEngine.class);

	public String registration(String body, String header, List<MultipartFile> files) {
		JSONObject displayConfig;

		JSONObject returnMessage = new JSONObject();
		String result = "";
		try {
			JSONObject jsonBody = new JSONObject(body);
			JSONObject headerdata = new JSONObject(header);
//			displayConfig = DisplaySingleton.memoryDispObjs2.getJSONObject("registrationTest");
			displayConfig = DisplaySingleton.memoryDispObjs2.getJSONObject("registrationForm");
			JSONObject jsonobj = new JSONObject(displayConfig.get("discfg").toString());
			JSONObject getdata = new JSONObject(displayConfig.get("datas").toString());
//			JSONObject datavalue = null;
			JSONObject currentdata = null;
			String primarykey = "";
			String primaryValue = "";
			String tablename = "";
			JSONObject oldJsonObje = null;
			JSONObject fetchData = null;

			String flowname = headerdata.getString("name");
			String registerProcessStep = headerdata.getString("mode");
			String requestFor = headerdata.has("requestFor") ? headerdata.getString("requestFor") : "";
			JSONArray findbyValue = headerdata.has("findby") ? headerdata.getJSONArray("findby") : new JSONArray();
			String comments = headerdata.has("comments") ? headerdata.getString("comments") : "";
			String NextStep = "";
			JSONObject currentFlow = null;

			JSONObject registerProcessStep1 = new JSONObject(jsonobj.getJSONObject(registerProcessStep).toString());

			Iterator<String> keys = registerProcessStep1.keys();

			while (keys.hasNext()) {
				String key = keys.next();
				JSONObject registerProcessLevels = new JSONObject(registerProcessStep1.getJSONObject(key).toString());
				if (registerProcessLevels.has(flowname)) {
					currentFlow = requestFor.equalsIgnoreCase("")
							? new JSONObject(registerProcessLevels.getJSONObject(flowname).toString())
							: new JSONObject(
									registerProcessLevels.getJSONObject(flowname).getJSONObject(requestFor).toString());
					tablename = currentFlow.getString("tablename");

					// FIND DATA IN TABLE ALREADY IS THERE!
					if (currentFlow.has("Find") && currentFlow.getBoolean("Find")) {
						currentdata = getdata.getJSONObject(flowname);
						LOGGER.info("User Check Start:::");
						JSONArray isData = searchdataisexsiting(currentdata, jsonBody);
						LOGGER.info("Find a Data End:::");
						if (isData != null && !isData.isEmpty()) {
							if (new JSONObject(isData.get(0).toString()).has("error"))
								return new JSONObject(isData.get(0).toString()).toString();

							return returnMessage.put("error", "This " + flowname + " Already Registered").toString();
						}
					}

					// IF YOU CAN ENCODE A PASSWORD
					if (currentFlow.has("passwordencode") && currentFlow.getBoolean("passwordencode")) {
						String userpass = jsonBody.has(currentFlow.getString("passwordkey"))
								? jsonBody.getString(currentFlow.getString("passwordkey"))
								: "ABCD@123";
						jsonBody.put(currentFlow.getString("passwordkey"), encoder.encode(userpass));
					}
					NextStep = currentFlow.getString("Nextstep");
					// ANY OTHER STATIC VALUE OR ELSE FETCH VALUE TO MAP A JSON
					if (currentFlow.has("mapvalues")
							&& !currentFlow.getJSONObject("mapvalues").getJSONArray("keys").isEmpty()) {
						// mapp by fecthcing table
						if (currentFlow.getJSONObject("mapvalues").has("mappedbyfetch")
								&& currentFlow.getJSONObject("mapvalues").getBoolean("mappedbyfetch")) {
							tablename = currentFlow.getString("tablename");

							currentdata = getdata
									.getJSONObject(currentFlow.getJSONObject("mapvalues").getString("Fetchby"));
							for (int i = 0; i < currentFlow.getJSONObject("mapvalues").getJSONArray("FetchKey")
									.length(); i++) {

								fetchData = new JSONObject();
								fetchData.put(currentFlow.getJSONObject("mapvalues").getJSONArray("FetchKey").get(i)
										.toString(), findbyValue.get(i));
							}
							JSONArray isData = searchdataisexsiting(currentdata, fetchData);
							if (isData.isEmpty()) {
								return returnMessage.put("error", "Failed to Registered,Please check and retry")
										.toString();
							}
							primarykey = currentdata.getJSONObject("primarykey").getString("columnname");
							primaryValue = new JSONObject(isData.get(0).toString())
									.get(currentdata.getJSONObject("primarykey").getString("columnname")).toString();

							// this one create a new jsonObject to Map.
							if (currentFlow.getJSONObject("mapvalues").getBoolean("JSONNew"))
								jsonBody = new JSONObject();

							jsonBody = maptojson(currentFlow.getJSONObject("mapvalues"),
									new JSONObject(isData.get(0).toString()),
									currentFlow.getJSONObject("mapvalues").has("oldjson") ? oldJsonObje
											: new JSONObject(),
									jsonBody);
							NextStep = currentFlow.getString("Nextstep");
						} else {// mapp to get a vlaue form json
							jsonBody = maptojson(currentFlow.getJSONObject("mapvalues"), jsonBody,
									currentFlow.getJSONObject("mapvalues").has("oldjson") ? oldJsonObje
											: new JSONObject(),
									new JSONObject());
							NextStep = currentFlow.getString("Nextstep");
						}
					}
					// Fetch data only
					if (currentFlow.has("FetchDatas")) {
						currentdata = getdata
								.getJSONObject(currentFlow.getJSONObject("FetchDatas").getString("Fetchby"));
						for (int i = 0; i < currentFlow.getJSONObject("FetchDatas").getJSONArray("Key").length(); i++) {

							fetchData = new JSONObject();
							fetchData.put(currentFlow.getJSONObject("FetchDatas").getJSONArray("Key").get(i).toString(),
									jsonBody.get(currentFlow.getJSONObject("FetchDatas").getJSONArray("Key").get(i)
											.toString()));
						}
						JSONArray isData = searchdataisexsiting(currentdata, fetchData);
						if (isData.isEmpty()) {
							return returnMessage.put("error", "Failed to Registered,Please check and retry").toString();
						}
						jsonBody = new JSONObject(isData.get(0).toString());
					}

					// CAN YOU THROUGH EMAIL
					if (currentFlow.has("Email")) {
						if (!comments.equalsIgnoreCase(""))
							jsonBody.put("comment", comments);
						JSONObject getJson = new JSONObject(currentFlow.getJSONObject("Email").toString());
						String Email = jsonBody.getString(getJson.getString("emailaddresscolumn"));
						JSONArray mailContentname = getJson.getJSONArray("mail");
						List<MultipartFile> filedata = new ArrayList<>();
						String restulofMail = amazonSMTPMail.mailSender2(mailContentname, Email, getJson, jsonBody,
								filedata, headerdata.has("lang") ? headerdata.getString("lang") : "en",
								GlobalAttributeHandler.getSchemas());
						NextStep = getJson.getString(restulofMail);
						if (getJson.getString(restulofMail).equalsIgnoreCase("Please retry Server was busy!")) {
							return new JSONObject()
									.put("error", "Email has been Failed to sent,Please Retry or check valid Email. ")
									.toString();
						}
					}

					// UPLOAD FILE
					if (currentFlow.has("file") && currentFlow.getBoolean("file")) {
						jsonBody = new JSONObject(fileuploadServices
								.fileupload(currentdata, files, jsonBody, new JSONObject()).toString());
					}
					// Remove extra columns in json
					if (currentFlow.has("expectedColumn") && !currentFlow.getJSONArray("expectedColumn").isEmpty()) {
						for (int e = 0; e < currentFlow.getJSONArray("expectedColumn").length(); e++) {
							System.err.println(currentFlow.getJSONArray("expectedColumn").toString());
							jsonBody.remove(currentFlow.getJSONArray("expectedColumn").get(e).toString());
						}

					}

					// LAST OPTION IS SAVE OR UPDATE
					if (NextStep.equalsIgnoreCase("save") || NextStep.equalsIgnoreCase("update")) {
						if (NextStep.equalsIgnoreCase("save")) {
							String url = GlobalAttributeHandler.getPgrestURL() + tablename;
							result = dataTransmit.transmitDataspgrestpost(url, jsonBody.toString(),
									currentFlow.has("returnSaveData") ? currentFlow.getBoolean("returnSaveData")
											: false,
											GlobalAttributeHandler.getSchemas());
						}
						if (NextStep.equalsIgnoreCase("update")) {
							String url = GlobalAttributeHandler.getPgrestURL() + tablename + "?" + primarykey + "=eq." + primaryValue;
							result = dataTransmit.transmitDataspgrestput(url, jsonBody.toString(),
									currentFlow.has("returnSaveData") ? currentFlow.getBoolean("returnSaveData")
											: false,
											GlobalAttributeHandler.getSchemas());
						}

						// handle the response
						if (result.startsWith("{")) {
							oldJsonObje = new JSONObject(result);
							if (oldJsonObje.has("code")) {
								return returnMessage.put("error", oldJsonObje.getString("message")).toString();
							}
							returnMessage.put("reflex",
									requestFor.equalsIgnoreCase("") ? "Successfully Registered"
											: requestFor.equalsIgnoreCase("rejected") ? "Rejected Successfully"
													: "Approved Successfully")
									.toString();
						} else if ((Integer.parseInt(result) >= 200 && Integer.parseInt(result) <= 226)) {
							oldJsonObje = jsonBody;
							returnMessage.put("reflex", "Successfully Registered").toString();
						} else {
							return new JSONObject().put("error", "Failed to Registered,Please check and retry")
									.toString();
						}
					}
					// Only get a value for mapp to another flow
					else if (NextStep.equalsIgnoreCase("getOldvalue")) {
						oldJsonObje = jsonBody;
					} else if (NextStep.equalsIgnoreCase("return")) {
						returnMessage.put("reflex",
								requestFor.equalsIgnoreCase("") ? "Successfully Registered"
										: requestFor.equalsIgnoreCase("rejected") ? "Rejected Successfully"
												: "Approved Successfully");
					}
				}
			}
		} catch (

		Exception e) {
			LOGGER.error("Exception At {}", Thread.currentThread().getStackTrace()[0].getMethodName(), e);
			returnMessage.put("error",
					"Something Went Wrong For Registration,Please Check Your data Or else Contact " + FromNameOfMail)
					.toString();
		}
		return returnMessage.toString();
	}

	public JSONObject maptojson(JSONObject mappingjson, JSONObject bodyjson, JSONObject oldValueJson,
			JSONObject jsonBody) {
		try {
			JSONArray keys = mappingjson.getJSONArray("keys");
			JSONArray values = mappingjson.getJSONArray("values");
			for (int i = 0; i < keys.length(); i++) {
				if (keys.get(i).toString().split("-")[0].equalsIgnoreCase("menu"))
					bodyjson.put(keys.get(i).toString().split("-")[1],
							DisplaySingleton.memoryApplicationSetting.get(values.get(i).toString()).toString());
				else {
					if (values.get(i).toString().startsWith("{"))
						bodyjson.put(keys.get(i).toString(), values.get(i).toString());
					else
						bodyjson.put(keys.get(i).toString(), values.get(i));
				}
			}
			if (!oldValueJson.isEmpty()) {
				keys = mappingjson.getJSONObject("oldjson").getJSONArray("keys");
				values = mappingjson.getJSONObject("oldjson").getJSONArray("getvalue");
				for (int i = 0; i < keys.length(); i++) {
					bodyjson.put(keys.get(i).toString(), oldValueJson.get(values.get(i).toString()));
				}
			}
			if (!jsonBody.isEmpty()) {
				Iterator<String> datakeys = jsonBody.keys();
				while (datakeys.hasNext()) {
					String key = datakeys.next();
					bodyjson.put(key, jsonBody.get(key));
				}

			}

		} catch (Exception e) {
			LOGGER.error("Exception At maptojson", e);
		}
		return bodyjson;

	}

	public JSONArray searchdataisexsiting(JSONObject getdata, JSONObject jsonBody) {
		String whereClass = "";
		String url = "";
		JSONArray isGetdata = null;
		try {
			if (getdata.getBoolean("function")) {
				for (int c = 0; c < getdata.getJSONObject("where").getJSONObject("functionWhere").getJSONArray("param")
						.length(); c++) {
					String param = getdata.getJSONObject("where").getJSONObject("functionWhere").getJSONArray("param")
							.get(c).toString();
					;
					String colunname = getdata.getJSONObject("where").getJSONObject("functionWhere")
							.getJSONArray("columnname").get(c).toString();
					if (colunname.split(",").length > 1) {

					} else {
						if (getdata.getJSONObject("where").getJSONObject("functionWhere").getBoolean("onlyvalue")) {
							if (c == 0)
								whereClass = "?" + param + "=" + jsonBody.get(getdata.getJSONObject("where")
										.getJSONObject("functionWhere").getJSONArray("value").get(c).toString());
						} else {
							String column = colunname.split("-")[0];
							String datatype = colunname.split("-")[1];
							if (c == 0) {
								whereClass = "?" + param + "=" + column + "=" + (datatype.equalsIgnoreCase("string")
										? "'" + jsonBody.get(getdata.getJSONObject("where")
												.getJSONObject("functionWhere").getJSONArray("value").get(c).toString())
												+ "'"
										: jsonBody.get(getdata.getJSONObject("where").getJSONObject("functionWhere")
												.getJSONArray("value").get(c).toString()));
							} else {
								whereClass = whereClass + "&" + param + "=" + column + "="
										+ (datatype.equalsIgnoreCase("string")
												? "'" + jsonBody.get(
														getdata.getJSONObject("where").getJSONObject("functionWhere")
																.getJSONArray("value").get(c).toString())
														+ "'"
												: jsonBody.get(
														getdata.getJSONObject("where").getJSONObject("functionWhere")
																.getJSONArray("value").get(c).toString()));
							}
						}
					}
					if (getdata.getJSONObject("where").has("defaultWhere")
							&& !getdata.getJSONObject("where").getString("defaultWhere").equalsIgnoreCase("")) {
						if (!whereClass.equalsIgnoreCase(""))
							url = (GlobalAttributeHandler.getPgrestURL() + getdata.getString("tablename") + whereClass + "&"
									+ getdata.getJSONObject("where").getString("defaultWhere")).replaceAll(" ", "%20");
						else
							url = (GlobalAttributeHandler.getPgrestURL() + getdata.getString("tablename") + "?"
									+ getdata.getJSONObject("where").getString("defaultWhere")).replaceAll(" ", "%20");
					} else {
						url = (GlobalAttributeHandler.getPgrestURL() + getdata.getString("tablename") + whereClass).replaceAll(" ", "%20");

					}
				}
			} else {
				for (int c = 0; c < getdata.getJSONObject("where").getJSONObject("pgWhere").getJSONArray("columnname")
						.length(); c++) {
					String colunname = getdata.getJSONObject("where").getJSONObject("pgWhere")
							.getJSONArray("columnname").get(c).toString();
					if (c == 0)
						whereClass = "?" + colunname + "=eq." + jsonBody.get(getdata.getJSONObject("where")
								.getJSONObject("pgWhere").getJSONArray("value").get(c).toString());
					else
						whereClass = whereClass + "&" + colunname + "=eq." + jsonBody.get(getdata.getJSONObject("where")
								.getJSONObject("pgWhere").getJSONArray("value").get(c).toString());
				}
				if (getdata.getJSONObject("where").has("defaultWhere")
						&& !getdata.getJSONObject("where").getString("defaultWhere").equalsIgnoreCase("")) {
					if (!whereClass.equalsIgnoreCase("")) {
						url = (GlobalAttributeHandler.getPgrestURL() + getdata.getString("tablename") + whereClass + "&"
								+ getdata.getJSONObject("where").getString("defaultWhere")).replaceAll(" ", "%20");
					} else {
						url = (GlobalAttributeHandler.getPgrestURL() + getdata.getString("tablename") + "?"
								+ getdata.getJSONObject("where").getString("defaultWhere")).replaceAll(" ", "%20");
					}
				} else {
					url = (GlobalAttributeHandler.getPgrestURL() + getdata.getString("tablename") + whereClass).replaceAll(" ", "%20");

				}
			}
			isGetdata = dataTransmit.transmitDataspgrest(url,GlobalAttributeHandler.getSchemas());
		} catch (Exception e) {
			LOGGER.error("Exception At Search Data Isexsiting data", e);
			isGetdata.put(new JSONObject().put("error", e.getMessage()));
		}
		return isGetdata;
	}

}
