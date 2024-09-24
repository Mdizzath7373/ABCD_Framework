package com.eit.abcdframework.service;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.eit.abcdframework.dto.CommonUtilDto;
import com.eit.abcdframework.http.caller.Httpclientcaller;
import com.eit.abcdframework.serverbo.CommonServices;
import com.eit.abcdframework.serverbo.DisplayConfigBO;
import com.eit.abcdframework.serverbo.DisplayHandler;
import com.eit.abcdframework.serverbo.DisplaySingleton;
import com.eit.abcdframework.serverbo.FileuploadServices;
import com.eit.abcdframework.serverbo.ResponcesHandling;
import com.eit.abcdframework.util.AmazonSMTPMail;
import com.eit.abcdframework.util.TimeZoneServices;
import com.eit.abcdframework.websocket.WebSocketService;

@Service
public class DCDesignDataServiceImpl implements DCDesignDataService {

	@Autowired
	DisplayConfigBO displayConfigBO;

	@Autowired
	Httpclientcaller dataTransmit;

	@Autowired
	AmazonSMTPMail amazonSMTPMail;

	@Autowired
	FileuploadServices fileuploadServices;

	@Autowired
	WebSocketService socketService;

	@Autowired
	CommonServices commonServices;

	@Autowired
	DisplayHandler displayHandler;

	@Value("${applicationurl}")
	private String pgresturl;

	private static final Logger LOGGER = LoggerFactory.getLogger("DCDesignDataServiceImpl");
//	private static final String ISSUSEFILE = "issusefile";

	private static final String KEY = "primarykey";
	private static final String primaryColumnKey = "columnname";
	private static final String REFLEX = "reflex";
	private static final String SUCCESS = "Success";
	private static final String ERROR = "error";
	private static final String FAILURE = "Failure";

	@Override
	public CommonUtilDto getDCDesignData(String data) {
		JSONObject obj = null;
		JSONObject values = null;
		String value = "";
		boolean function = false;
		String role = "";
		try {
			JSONObject objData = new JSONObject(data);
			if (!objData.has("callback"))
				obj = new JSONObject(CommonServices.decrypt(objData.getString("data")));
			else
				obj = new JSONObject(data);

			values = obj.getJSONObject("value");
			value = values.toString();
			function = obj.getBoolean("function");
			role = obj.has("role") ? obj.getString("role") : "default";
		} catch (Exception e) {
			LOGGER.error(Thread.currentThread().getStackTrace()[0].getMethodName(), e);
		}
		return displayConfigBO.getDesignData(value, function, role);
	}

	@Override
	public String fileUpload(List<MultipartFile> files, String data, String transmitMethod) {
		JSONObject jsonObject1 = null;
		JSONObject documentdata = null;
		String res = "";
		try {
			int primary_id = 0;
			if (data.equalsIgnoreCase("") && !data.startsWith("{")) {
				return "Please Check Your Data Object!";
			}
			JSONObject displayConfig;
			if (!data.startsWith("{"))
				jsonObject1 = new JSONObject(CommonServices.decrypt(data));
			else
				jsonObject1 = new JSONObject(data);

			JSONObject jsonheader = new JSONObject(
					jsonObject1.getJSONObject("PrimaryBody").getJSONObject("header").toString());
			String displayAlias = jsonheader.getString("name");
			String method = jsonheader.getString("method");

			JSONObject jsonbody = new JSONObject(
					jsonObject1.getJSONObject("PrimaryBody").getJSONObject("body").toString());

			displayConfig = DisplaySingleton.memoryDispObjs2.getJSONObject(displayAlias);
			JSONObject gettabledata = new JSONObject(displayConfig.get("datas").toString());

			boolean isprogress = false;

			if (transmitMethod.equalsIgnoreCase("Upload")) {
				fileuploadServices.fileupload(gettabledata, files, jsonbody, documentdata);
			} else if (transmitMethod.equalsIgnoreCase("UploadWithProgress")) {
				res = fileuploadServices.convertPdfToMultipart(files.get(0), gettabledata, jsonbody);
				isprogress = true;
				if (res.equalsIgnoreCase("Failed")) {
					new JSONObject().put(ERROR, FAILURE).toString();
				}
			} else if (transmitMethod.equalsIgnoreCase("MergeFile")) {
				fileuploadServices.mergebase64ToPDF(gettabledata, jsonbody, files.get(0));
			}

			toSaveObject(method, jsonbody, gettabledata, jsonheader, files);

			if (gettabledata.has("synchronizedCurdOperation")) {
				JSONArray typeOfMehods = gettabledata.getJSONObject("synchronizedCurdOperation").getJSONArray("type");
				for (int typeOfMehod = 0; typeOfMehod < typeOfMehods.length(); typeOfMehod++) {
					String councurrentAPIres = "";
					if (typeOfMehods.get(typeOfMehod).toString().equalsIgnoreCase("Map")) {
						councurrentAPIres = CommonServices.MappedCurdOperation(gettabledata, data);
						LOGGER.info("Councurrent API Response----->{}", councurrentAPIres);
					}
					if (!councurrentAPIres.equalsIgnoreCase("Sucess")) {
						return new JSONObject().put(ERROR, FAILURE).toString();
					}
				}
			}

			if (isprogress) {
				Map<String, AtomicInteger> progress = fileuploadServices.getProgress();
				progress.put(jsonbody.get("ids").toString() + "-" + (jsonbody
						.get(gettabledata.getJSONObject("Splitter").getString("Splitter_primary_id")).toString()),
						new AtomicInteger(100));
				fileuploadServices.setProgress(progress);

				socketService.pushSocketData(jsonheader, jsonbody, "progress");
			}

			if (primary_id != 0) {
				String delUrl = pgresturl + "pdf_splitter?id=eq." + primary_id;
				dataTransmit.transmitDataspgrestDel(delUrl);
			} else {
				new JSONObject().put(ERROR, FAILURE).toString();
			}

		} catch (Exception e) {
			LOGGER.error(Thread.currentThread().getStackTrace()[0].getMethodName(), e);
		}
		return new JSONObject().put(REFLEX, SUCCESS).toString();
	}

	private String toSaveObject(String method, JSONObject jsonbody, JSONObject gettabledata, JSONObject jsonheader,
			List<MultipartFile> files) {
		String res = "";
		try {
			String response = "";
			String url = "";
			String columnprimarykey = gettabledata.getJSONObject(KEY).getString(primaryColumnKey);

			if (method.equalsIgnoreCase("POST")) {
				if (gettabledata.has("dateandtime")) {
					jsonbody.put(gettabledata.getString("dateandtime"),
							TimeZoneServices.getDateInTimeZoneforSKT("Asia/Riyadh"));
				}
				url = pgresturl + gettabledata.getString("api").replaceAll(" ", "%20");
				response = dataTransmit.transmitDataspgrestpost(url, jsonbody.toString(), false);
			} else if (method.equalsIgnoreCase("PUT")) {
				if (jsonbody.has(columnprimarykey) && !jsonbody.get(columnprimarykey).toString().equalsIgnoreCase("")) {
					// if use put method we need primary key (set primary key column name)
					url = (pgresturl + gettabledata.getString("api") + "?" + columnprimarykey + "=eq."
							+ (jsonbody.get(columnprimarykey)).toString()).replaceAll(" ", "%20");
					response = dataTransmit.transmitDataspgrestput(url, jsonbody.toString(), false);
				} else {
					return new JSONObject().put(ERROR, "primaryKey is Missing,Please Check this").toString();
				}
			}

			res = ResponcesHandling.curdMethodResponceHandle(response, jsonbody, jsonheader, gettabledata, method,
					files);

		} catch (Exception e) {
			LOGGER.error(Thread.currentThread().getStackTrace()[0].getMethodName(), e);
		}
		return res;
	}

//	@Override
	public String fileupload(List<MultipartFile> files, String data) {
		JSONObject returndata = new JSONObject();
//		JSONObject email = null;
		String res = "";
		JSONObject documentdata = null;
		JSONObject jsonObject1 = null;
		try {
			JSONObject displayConfig;
			if (!data.startsWith("{"))
				jsonObject1 = new JSONObject(CommonServices.decrypt(data));
			else
				jsonObject1 = new JSONObject(data);

			JSONObject jsonheader = new JSONObject(jsonObject1.getJSONObject("header").toString());
			if (jsonObject1.has("documents")) {
				documentdata = new JSONObject(jsonObject1.getJSONObject("documents").toString());
			} else {
				documentdata = new JSONObject();
			}
			String displayAlias = jsonheader.getString("name");
			String method = jsonheader.getString("method");

			JSONObject jsonbody = new JSONObject(jsonObject1.getJSONObject("body").toString());

			displayConfig = DisplaySingleton.memoryDispObjs2.getJSONObject(displayAlias);
			JSONObject gettabledata = new JSONObject(displayConfig.get("datas").toString());
			// primary key column name comes in table
			String columnprimarykey = gettabledata.getJSONObject("primarykey").getString("columnname");
			// File Upload Class
			JSONObject setJsonBody = new JSONObject();
			setJsonBody = fileuploadServices.fileupload(gettabledata, files, jsonbody, documentdata);

			if (setJsonBody.isEmpty() || setJsonBody.has("error")) {
				return returndata.put(ERROR, setJsonBody.getString("error")).toString();
			}
			// To save a Data.
			String url = "";
			String response = "";
			if (method.equalsIgnoreCase("POST")) {
				if (gettabledata.has("dateandtime")) {
					jsonbody.put(gettabledata.getString("dateandtime"),
							TimeZoneServices.getDateInTimeZoneforSKT("Asia/Riyadh"));
				}
				url = pgresturl + gettabledata.getString("api");
				url = url.replace(" ", "%20");
				response = dataTransmit.transmitDataspgrestpost(url, jsonbody.toString(), false);
			} else if (method.equalsIgnoreCase("PUT")) {
				if (jsonbody.has(columnprimarykey) && !jsonbody.get(columnprimarykey).toString().equalsIgnoreCase("")) {
					// if use put method we need primary key (set primary key column name)
					url = pgresturl + gettabledata.getString("api") + "?" + columnprimarykey + "=eq."
							+ (jsonbody.get(columnprimarykey)).toString();
					url = url.replace(" ", "%20");
					response = dataTransmit.transmitDataspgrestput(url, jsonbody.toString(), false);
				} else {
					return returndata.put(ERROR, "primaryKey is Missing,Please Check this").toString();
				}
			}

			res = ResponcesHandling.curdMethodResponceHandle(response, jsonbody, jsonheader, gettabledata, method,
					files);

		} catch (

		Exception e) {
			LOGGER.error("Exception at fileupload" + Thread.currentThread().getStackTrace()[1].getMethodName(), e);
			return new JSONObject().put(ERROR, "Failure").toString();
		}
		LOGGER.info("Fileupload Completed");
//		return returndata.toString();
		return res;
	}

	@Override
	public String getwidgetsdata(String data) {
		String aliesname = "";
		boolean function = false;
		String role = "";
		String where = "";
		JSONObject getjson = null;
		try {
			JSONObject objData = new JSONObject(data);
			if (!objData.has("header"))
				getjson = new JSONObject(CommonServices.decrypt(objData.getString("data")));
			else
				getjson = new JSONObject(data);
			JSONObject header = new JSONObject(getjson.getJSONObject("header").toString());
//          method = header.getString("method");
			function = header.getBoolean("function");
			if (header.has("role")) {
				role = header.getString("role");
			} else {
				role = "default";
			}
			aliesname = header.getString("Name");
			where = header.has("where") ? header.getString("where") : "";

		} catch (Exception e) {
			LOGGER.error("Exception at " + Thread.currentThread().getStackTrace()[1].getMethodName(), e);
		}
		return getwidgets(aliesname, function, where, role);
	}

	public String getwidgets(String aliesname, boolean function, String where, String role) {
		JSONObject displayConfig;
		JSONObject returnJson = new JSONObject();
		try {
			List<JSONObject> checkjson = new ArrayList<>();
			String url = "";
			JSONArray datavalues = null;
			String regex = "[^a-zA-Z0-9=&?_  -><\\u0600-\\u06FF]";// DisplaySingleton.memoryApplicationSetting.getString("UrlEncodeExcept");
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
			where = result.toString().replaceAll("#", "%23");

			displayConfig = DisplaySingleton.memoryDispObjs2.getJSONObject(aliesname);
			String method = function ? "Function" : "api";
			if (displayConfig != null) {
				JSONObject displayData = new JSONObject(displayConfig.get("discfg").toString());
				JSONObject extraDatas = new JSONObject(displayConfig.get("datas").toString());
				JSONObject jsononbj = new JSONObject(displayData.getJSONObject("jqxdetails").toString());
				JSONArray jsonArray = new JSONArray(jsononbj.getJSONArray(extraDatas.getString("name")).toString());

				returnJson.put("jqxdetails", jsononbj.toString());
				if (function && extraDatas.has("preDefined") && extraDatas.getBoolean("preDefined")) {
					url = pgresturl + extraDatas.getString("Function") + "?basequery=" + extraDatas.getJSONObject("Query");
				} else {
					url = pgresturl + extraDatas.getString(method);
				}

				if (where != "" && extraDatas.has("params")) {
					String params = "&";
					for (int i = 0; i < extraDatas.getJSONArray("params").length(); i++) {
						params += extraDatas.getJSONArray("params").get(i).toString() + "="
								+ extraDatas.getJSONArray("value").get(i).toString();
					}
					url += where + params;
				} else if (extraDatas.has("params")) {
					String params = "?";
					for (int i = 0; i < extraDatas.getJSONArray("params").length(); i++) {
						params += extraDatas.getJSONArray("params").get(i).toString() + "="
								+ extraDatas.getJSONArray("value").get(i).toString();
					}
					url += params;
				} else if (where != "") {
					url += where;
				}
				url = url.replace(" ", "%20");
				if (extraDatas.has("preDefined") && extraDatas.getBoolean("preDefined")) {
					datavalues = new JSONObject(new JSONArray(dataTransmit.transmitDataspgrest(url).get(0).toString()))
							.getJSONArray("datavalues");
				} else {
					datavalues = dataTransmit.transmitDataspgrest(url);
				}
//				datavalues = dataTransmit.transmitDataspgrest(url);
				returnJson.put("datavalue", datavalues.toString());
				
				JSONObject coloumnsnew = new JSONObject();
				if (role != null && !role.equalsIgnoreCase("")) {
					JSONArray datas = jsononbj.getJSONArray(extraDatas.getString("name"));
					List<Object> showgirddata = new JSONObject(jsononbj.get("showwidgetsbyrole").toString())
							.getJSONArray(role).toList();
					for (int i = 0; i < datas.length(); i++) {
						if (showgirddata.contains(new JSONObject(datas.get(i).toString()).getString("widgetname"))) {
							JSONObject dataObj = (JSONObject) jsonArray.get(i);
							coloumnsnew.put(dataObj.getString("widgetname"), dataObj.getString("displayfield"));
							checkjson.add(new JSONObject(datas.get(i).toString()));
						}

					}
					returnJson.put(extraDatas.getString("name"), coloumnsnew.toString());
					jsononbj.put(extraDatas.getString("name"), checkjson);
				}
				returnJson.put("jqxdetails", jsononbj.toString());
			}

		} catch (Exception e) {
			returnJson.put(ERROR, "Something went worng, Please Retry");
			LOGGER.error("Exception at ", e);
		}
		return returnJson.toString();
	}

	@Override
	public String getDCDesignChart(String data) {
		JSONObject obj = null;
		JSONObject values = null;
		String chartType = "";
		String value = "";
		boolean function = false;
		String role = "";
		try {
			JSONObject objData = new JSONObject(data);
			if (!objData.has("callback"))
				obj = new JSONObject(CommonServices.decrypt(objData.getString("data")));
			else
				obj = new JSONObject(data);

			values = obj.getJSONObject("value");
			chartType = obj.getString("chartType");
			value = values.toString();
			function = obj.getBoolean("function");
			role = obj.has("role") ? obj.getString("role") : "default";
		} catch (Exception e) {
			LOGGER.error(Thread.currentThread().getStackTrace()[0].getMethodName(), e);
		}
		return displayHandler.toExecutePgRest(value, function, role, chartType);
	}

	@Override
	public void getProgress(String data) {

		JSONObject value = new JSONObject(data);
		List<Object> key = value.getJSONArray("id").toList();
		String companyid = value.getString("companyId");

		List<String> keysToRemove = key.stream().map(entry -> companyid + "-" + entry).collect(Collectors.toList());

		Map<String, AtomicInteger> filteredMap = fileuploadServices.getProgress().entrySet().stream()
				.filter(entry -> !keysToRemove.contains(entry.getKey()))
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

		fileuploadServices.getProgress().clear();
		fileuploadServices.setProgress(filteredMap);

	}

//	@Override
	public String fileuploadwithprogress(MultipartFile files, String data) {
		return data;
//		JSONObject jsonObject1 = null;
//		JSONObject returndata = new JSONObject();
//		try {
//			if (data.equalsIgnoreCase("") && !data.startsWith("{")) {
//				return "Please Check Your Data Object!";
//			}
//			JSONObject displayConfig;
//			if (!data.startsWith("{"))
//				jsonObject1 = new JSONObject(CommonServices.decrypt(data));
//			else
//				jsonObject1 = new JSONObject(data);
//
//			JSONObject jsonheader = new JSONObject(
//					jsonObject1.getJSONObject("PrimaryBody").getJSONObject("header").toString());
//			String displayAlias = jsonheader.getString("name");
//			String method = jsonheader.getString("method");
//
//			JSONObject jsonbody = new JSONObject(
//					jsonObject1.getJSONObject("PrimaryBody").getJSONObject("body").toString());
//
//			displayConfig = DisplaySingleton.memoryDispObjs2.getJSONObject(displayAlias);
//			JSONObject gettabledata = new JSONObject(displayConfig.get("datas").toString());
//			// primary key column name comes in table
//			String columnprimarykey = gettabledata.getJSONObject("primarykey").getString("columnname");
//			String Splitter_primary_id = gettabledata.getJSONObject("Splitter").getString("Splitter_primary_id");
//
//			String value = jsonbody.get(Splitter_primary_id).toString();
//			fileuploadServices.convertPdfToMultipart(files, value, jsonbody.get("ids").toString(), jsonbody);
//
//			String url = "";
//			String res = "";
//			String response = "";
//			if (method.equalsIgnoreCase("POST")) {
//				if (gettabledata.has("dateandtime")) {
//					jsonbody.put(gettabledata.getString("dateandtime"),
//							TimeZoneServices.getDateInTimeZoneforSKT("Asia/Riyadh"));
//				}
//				url = pgresturl + gettabledata.getString("api");
//				url = url.replace(" ", "%20");
//				response = dataTransmit.transmitDataspgrestpost(url, jsonbody.toString(), false);
//			} else if (method.equalsIgnoreCase("PUT")) {
//				if (jsonbody.has(columnprimarykey) && !jsonbody.get(columnprimarykey).toString().equalsIgnoreCase("")) {
//					// if use put method we need primary key (set primary key column name)
//					url = pgresturl + gettabledata.getString("api") + "?" + columnprimarykey + "=eq."
//							+ (jsonbody.get(columnprimarykey)).toString();
//					url = url.replace(" ", "%20");
//					response = dataTransmit.transmitDataspgrestput(url, jsonbody.toString(), false);
//				} else {
//					return returndata.put(ERROR, "primaryKey is Missing,Please Check this").toString();
//				}
//			}
//
//			if (Integer.parseInt(response) >= 200 && Integer.parseInt(response) <= 226) {
//				if (gettabledata.has("synchronizedCurdOperation")) {
//					JSONArray typeOfMehods = gettabledata.getJSONObject("synchronizedCurdOperation")
//							.getJSONArray("type");
//					for (int typeOfMehod = 0; typeOfMehod < typeOfMehods.length(); typeOfMehod++) {
//						if (typeOfMehods.get(typeOfMehod).toString().equalsIgnoreCase("Map")) {
//							res = CommonServices.MappedCurdOperation(gettabledata, data);
//						}
//					}
//					if (res.equalsIgnoreCase("Success"))
//						returndata.put("reflex", res);
//					else
//						return returndata.put("error", res).toString();
//				}
//
//				Map<String, AtomicInteger> progress = fileuploadServices.getProgress();
//				progress.put(jsonbody.get("ids").toString() + "-" + value, new AtomicInteger(100));
//				fileuploadServices.setProgress(progress);
//
//				socketService.pushSocketData(jsonheader, jsonbody, "progress");
//
//			}
//
//		} catch (Exception e) {
//			LOGGER.error(Thread.currentThread().getStackTrace()[0].getMethodName(), e);
//			return new JSONObject().put("error", "Failed Please Retry").toString();
//		}
//
//		return returndata.toString();
	}

//	@Override
	public String mergeToPDF(MultipartFile files, String data) {
		JSONObject jsonObject1 = null;
		JSONObject returndata = new JSONObject();
		try {

			if (data.equalsIgnoreCase("") && !data.startsWith("{")) {
				return "Please Check Your Data Object!";
			}
			JSONObject displayConfig;
			if (!data.startsWith("{"))
				jsonObject1 = new JSONObject(CommonServices.decrypt(data));
			else
				jsonObject1 = new JSONObject(data);

			JSONObject jsonheader = new JSONObject(
					jsonObject1.getJSONObject("PrimaryBody").getJSONObject("header").toString());
			String displayAlias = jsonheader.getString("name");
			String method = jsonheader.getString("method");

			JSONObject jsonbody = new JSONObject(
					jsonObject1.getJSONObject("PrimaryBody").getJSONObject("body").toString());

			displayConfig = DisplaySingleton.memoryDispObjs2.getJSONObject(displayAlias);
			JSONObject gettabledata = new JSONObject(displayConfig.get("datas").toString());

			JSONArray jarr = new JSONArray(gettabledata.getJSONArray("filepathname").toString());

			// set list of column name in json body (column name)
			JSONArray column = new JSONArray(gettabledata.getJSONArray("column").toString());
			String filename = jsonbody.get(jarr.get(0).toString()).toString();

			// primary key column name comes in table
			String columnprimarykey = gettabledata.getJSONObject("primarykey").getString("columnname");

			String Splitter_primary_id = gettabledata.getJSONObject("Splitter").getString("Splitter_primary_id");

			String value = jsonbody.get(Splitter_primary_id).toString();

			String geturl = pgresturl + "pdf_splitter?select=total_pages,id&primary_id_pdf=eq." + value;
			JSONObject datavalue = new JSONObject(dataTransmit.transmitDataspgrest(geturl).get(0).toString());
			int total_pages = datavalue.getInt("total_pages");
			int primary_id = datavalue.getInt("id");

			String currentDir = System.getProperty("user.dir");
			String PDFpath = "";

			if (gettabledata.getJSONObject("Splitter").has("original")
					&& gettabledata.getJSONObject("Splitter").getBoolean("original")) {
				PDFpath = currentDir + "original.pdf";
			}

			Map<String, Object> base64Images = commonServices.loadBase64(value, total_pages);
			JSONObject path = null;
			;
			try (PDDocument document = new PDDocument()) {
				path = fileuploadServices.writeImage(base64Images, PDFpath, filename, document, files);
				if (path.has("error")) {
					return new JSONObject().put("error", "Failed to upload s3bucket!").toString();
				}
				jsonbody.put(column.get(0).toString(), path);
			}

			String url = "";
			String res = "";
			String response = "";
			if (method.equalsIgnoreCase("POST")) {
				if (gettabledata.has("dateandtime")) {
					jsonbody.put(gettabledata.getString("dateandtime"),
							TimeZoneServices.getDateInTimeZoneforSKT("Asia/Riyadh"));
				}
				url = pgresturl + gettabledata.getString("api");
				url = url.replace(" ", "%20");
				response = dataTransmit.transmitDataspgrestpost(url, jsonbody.toString(), false);
			} else if (method.equalsIgnoreCase("PUT")) {
				if (jsonbody.has(columnprimarykey) && !jsonbody.get(columnprimarykey).toString().equalsIgnoreCase("")) {
					// if use put method we need primary key (set primary key column name)
					url = pgresturl + gettabledata.getString("api") + "?" + columnprimarykey + "=eq."
							+ (jsonbody.get(columnprimarykey)).toString();
					url = url.replace(" ", "%20");
					response = dataTransmit.transmitDataspgrestput(url, jsonbody.toString(), false);
				} else {
					return returndata.put(ERROR, "primaryKey is Missing,Please Check this").toString();
				}
			}

			if (Integer.parseInt(response) >= 200 && Integer.parseInt(response) <= 226) {
				if (gettabledata.has("synchronizedCurdOperation")) {
					JSONArray typeOfMehods = gettabledata.getJSONObject("synchronizedCurdOperation")
							.getJSONArray("type");
					for (int typeOfMehod = 0; typeOfMehod < typeOfMehods.length(); typeOfMehod++) {
						if (typeOfMehods.get(typeOfMehod).toString().equalsIgnoreCase("Map")) {
							res = CommonServices.MappedCurdOperation(gettabledata, data);
						}
					}
					if (res.equalsIgnoreCase("Success"))
						returndata.put("reflex", res);
					else
						returndata.put("error", res);
				}

				if (returndata.has("reflex")) {
					String delUrl = pgresturl + "pdf_splitter?id=eq." + primary_id;
					dataTransmit.transmitDataspgrestDel(delUrl);
				}

			}

		} catch (Exception e) {
			LOGGER.error(Thread.currentThread().getStackTrace()[0].getMethodName(), e);
			return new JSONObject().put("error", "Failed Please Retry").toString();
		}
		return returndata.toString();

	}

	@Override
	public String SplitterPDFChanges(JSONObject jsonObject1) {
		JSONObject res = new JSONObject();
		try {
			JSONObject setValues = new JSONObject();

			JSONObject jsonObject = jsonObject1.getJSONObject("document");

			jsonObject.keys().forEachRemaining(key -> {
				String url = pgresturl + "rpc/update_base64";
				JSONObject jsondata = new JSONObject();
				jsondata.put("key", key);
				jsondata.put("datavalue", jsonObject.getString(key));
				jsondata.put("primary", jsonObject1.get("id"));

				setValues.put("datas", jsondata);

				dataTransmit.transmitDataspgrestpost(url, setValues.toString(), false);

			});
			res.put("reflex", "Success");

		} catch (Exception e) {
			LOGGER.error(Thread.currentThread().getStackTrace()[0].getMethodName(), e);
			return new JSONObject().put("error", "Failed Please Retry").toString();
		}

		return res.toString();

	}
}