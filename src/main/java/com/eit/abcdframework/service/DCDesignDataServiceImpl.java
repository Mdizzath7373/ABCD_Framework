package com.eit.abcdframework.service;

import java.io.File;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.apache.commons.httpclient.HttpStatus;
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
	private static final String ERROR = "error";
//	private static final String ISSUSEFILE = "issusefile";

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
//			String res = "";
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

			res = ResponcesHandling.curdMethodResponceHandle(response, jsonbody,jsonheader, gettabledata, method,files);

//			if (Integer.parseInt(response) >= 200 && Integer.parseInt(response) <= 226) {
//
//				String socketRes = socketService.pushSocketData(jsonheader, jsonbody, method);
//				if (!socketRes.equalsIgnoreCase("Success")) {
//					LOGGER.error("Push Socket responce::{}", socketRes);
//				}
//				if (gettabledata.has("email")) {
//					email = new JSONObject(gettabledata.get("email").toString());
//					System.err.println(email.getJSONObject("mail").isEmpty());
//					if (!email.getJSONObject("mail").isEmpty())
//						amazonSMTPMail.emailconfig(email, jsonbody, files,
//								jsonheader.has("lang") ? jsonheader.getString("lang") : "en", method);
//				}
//				if (gettabledata.has("activityLogs")) {
//					String resp = "";
//					if (jsonheader.has("message") && jsonheader.getString("message").equalsIgnoreCase("")
//							&& jsonheader.has("status") && jsonheader.getString("status").equalsIgnoreCase("")) {
//						resp = commonServices.addactivitylog(gettabledata.getJSONObject("activityLogs"),
//								jsonheader.getString("status"), jsonbody,
//								jsonheader.has("rolename") ? jsonheader.getString("rolename") : "",
//								jsonheader.getString("message"),
//								jsonheader.has("notification") ? jsonheader.getBoolean("notification") : false);
//					}
//					LOGGER.error("ActivityLogs-->:: {}", resp);
//				}
//				returndata.put("reflex", "Success");
//			} else {
//				res = HttpStatus.getStatusText(Integer.parseInt(response));
//				returndata.put(ERROR, res);
//			}

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
			String regex = "[^a-zA-Z0-9=&?_  -><]";// DisplaySingleton.memoryApplicationSetting.getString("UrlEncodeExcept");
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
			String method = function ? "Function" : "GET";
			if (displayConfig != null) {
				JSONObject displayData = new JSONObject(displayConfig.get("discfg").toString());
				JSONObject extraDatas = new JSONObject(displayConfig.get("datas").toString());
				JSONObject jsononbj = new JSONObject(displayData.getJSONObject("jqxdetails").toString());
				JSONArray jsonArray = new JSONArray(jsononbj.getJSONArray(extraDatas.getString("name")).toString());

				returnJson.put("jqxdetails", jsononbj.toString());
				url = pgresturl + extraDatas.getString(method);
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
				datavalues = dataTransmit.transmitDataspgrest(url);
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

	public String transmittingDatatopgrest(String primarykey, String url, String method, JSONObject gettabledata,
			JSONObject jsonbody, String where) {
		JSONObject returndata = new JSONObject();
		String response = "";
		String res = "";
		JSONArray temparay;
		try {
			if (jsonbody.has(primarykey) && !jsonbody.get(primarykey).toString().equalsIgnoreCase("")) {
				if (method.equalsIgnoreCase("PUT")) {
					url = url + gettabledata.getString("api") + "?" + primarykey + "=eq." + jsonbody.get(primarykey);
					url = url.replace(" ", "%20");
					method = method.toUpperCase();
					response = dataTransmit.transmitDataspgrestput(url, jsonbody.toString(), false);
				} else if (method.equalsIgnoreCase("GET")) {
					url = url + gettabledata.getString(method.toUpperCase()) + "?" + primarykey + "=eq."
							+ jsonbody.get(primarykey);
					url = url.replace(" ", "%20");
					method = method.toUpperCase();
					temparay = dataTransmit.transmitDataspgrest(url);
					returndata.put("datavalue", temparay);
				} else if (method.equalsIgnoreCase("post")) {
					url = url + gettabledata.getString("api");
					url = url.replace(" ", "%20");
					method = method.toUpperCase();
					response = dataTransmit.transmitDataspgrestpost(url, jsonbody.toString(), false);
				}
			} else {
				if (!jsonbody.has(primarykey) && !where.equalsIgnoreCase("")) {
					url = url + gettabledata.getString(method.toUpperCase()) + where;
					dataTransmit.transmitDataspgrest(url);
				} else {
					url = url + gettabledata.getString("api");
					dataTransmit.transmitDataspgrest(url);
				}
			}
			method = method.toUpperCase();

			if (!method.equalsIgnoreCase("GET")) {
				if (response.equalsIgnoreCase("success")) {
					returndata.put("reflex", "Success");
				} else if (Integer.parseInt(response) >= 200 && Integer.parseInt(response) <= 226) {
					returndata.put("reflex", "Success");
				} else {
					res = HttpStatus.getStatusText(Integer.parseInt(response));
					returndata.put(ERROR, res);
				}
			}

		} catch (Exception e) {
			returndata.put(ERROR, "Failure");
			LOGGER.error("Exception at " + Thread.currentThread().getStackTrace()[1].getMethodName(), e);
		}
		return returndata.toString();
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

	@Override
	public String fileuploadwithprogress(MultipartFile files, String data) {
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
			// primary key column name comes in table
			String columnprimarykey = gettabledata.getJSONObject("primarykey").getString("columnname");
			String Splitter_primary_id = gettabledata.getJSONObject("Splitter").getString("Splitter_primary_id");

			String value = jsonbody.get(Splitter_primary_id).toString();
			fileuploadServices.convertPdfToMultipart(files, value, jsonbody.get("ids").toString(), jsonbody);

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
							res = ResponcesHandling.MappedCurdOperation(gettabledata, data);
						}
					}
					returndata.put("reflex", res);
				}

				Map<String, AtomicInteger> progress = fileuploadServices.getProgress();
				progress.put(jsonbody.get("ids").toString() + "-" + value, new AtomicInteger(100));
				fileuploadServices.setProgress(progress);

				 socketService.pushSocketData(jsonheader, jsonbody, "progress");

			}

		} catch (Exception e) {
			LOGGER.error(Thread.currentThread().getStackTrace()[0].getMethodName(), e);
		}

		return returndata.toString();
	}

	@Override
	public String mergeToPDF(MultipartFile files,String data) {
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

			String currentDir = System.getProperty("user.dir");

			String nameofPDF = "output.pdf";

			String PDFpath = currentDir + nameofPDF;

			Map<String, Object> base64Images = commonServices.loadBase64(value);
			String path = "";
			try (PDDocument document = new PDDocument()) {
				path = fileuploadServices.writeImage(base64Images, PDFpath, filename,document,files);
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
							res = ResponcesHandling.MappedCurdOperation(gettabledata, data);
						}
					}
					returndata.put("reflex", res);
				}

			}

			// for (Map.Entry<String, Object> entry : base64Images.entrySet()) {
//
//				String imageName = entry.getKey();
//				String base64Image = (String) entry.getValue();
//				byte[] imageBytes = Base64.getDecoder().decode(base64Image.replace("data:image/png;base64,", ""));
//
//				ByteArrayInputStream bis = new ByteArrayInputStream(imageBytes);
//				BufferedImage bufferedImage = ImageIO.read(bis);
//
//				File outputfile = new File("F:\\images\\" + imageName + ".jpg");
//				ImageIO.write(bufferedImage, "JPEG", outputfile);
//
//				// Image to be added
//				String imagePath = "F:\\images\\" + imageName + ".jpg";
//
//				PDPage pages = new PDPage(PDRectangle.A4);
//				document.addPage(pages);
//
//				PDImageXObject images = PDImageXObject.createFromFile(imagePath, document);
//
//				float width = pages.getMediaBox().getWidth();
//				float height = pages.getMediaBox().getHeight();
//
//				float imageWidth = images.getWidth();
//				float imageHeight = images.getHeight();
//
//				float scaleFactor = Math.min(width / imageWidth, height / imageHeight);
//
//				float scaledWidth = imageWidth * scaleFactor;
//				float scaledHeight = imageHeight * scaleFactor;
//
//				float x = (width - scaledWidth) / 2;
//				float y = (height - scaledHeight) / 2;
//
//				PDPageContentStream contentStream = new PDPageContentStream(document, pages);
//
//				contentStream.drawImage(images, x, y, width, height);
//				contentStream.close();
//			}
//			document.save(PDFpath);
			System.err.println("compaleted");

		} catch (Exception e) {
			LOGGER.error(Thread.currentThread().getStackTrace()[0].getMethodName(), e);
		}
		return data;

	}

	@Override
	public String SplitterPDFChanges(String data) {
		JSONObject jsonObject1 = new JSONObject();
		String res = "";
		try {

			if (data.equalsIgnoreCase("") && !data.startsWith("{")) {
				return "Please Check Your Data Object!";
			}
			if (!data.startsWith("{"))
				jsonObject1 = new JSONObject(CommonServices.decrypt(data));
			else
				jsonObject1 = new JSONObject(data);

			Map<String, Object> base64Images = commonServices.loadBase64(jsonObject1.getString("primary_id_pdf"));
			JSONObject jsonObject = jsonObject1.getJSONObject("document");

			jsonObject.keys().forEachRemaining(key -> {
				base64Images.put(key, jsonObject.getString(key));
			});
			jsonObject1.put("document", base64Images);

			String url = pgresturl + "pdf_splitter?id=eq." + jsonObject1.get("id");
			res = dataTransmit.transmitDataspgrestput(url, jsonObject1.toString(), false);

		} catch (Exception e) {
			LOGGER.error(Thread.currentThread().getStackTrace()[0].getMethodName(), e);
		}

		return res;

	}

	@Override
	public String fileuploadforgeneratedpdf(String base64, String data) {

		JSONObject returndata = new JSONObject();
		try {

//			SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy hh:mm:ss");
//			JSONObject email = null;
//			JSONArray mail = null;
//
//			JSONObject displayConfig;
//			JSONObject jsonObject1 = new JSONObject(data);
//
//			JSONObject jsonheader = new JSONObject(jsonObject1.getJSONObject("header").toString());
//			String displayAlias = jsonheader.getString("name");
//			String method = jsonheader.getString("method");
//
//			JSONObject jsonbody = new JSONObject(jsonObject1.getJSONObject("body").toString());
////			String s3url = DisplaySingleton.memoryApplicationSetting.get("s3url").toString();
////			String pgresturl = DisplaySingleton.memoryApplicationSetting.get("pgresturl").toString();
//
//			displayConfig = DisplaySingleton.memoryDispObjs2.getJSONObject(displayAlias);
//			JSONObject gettabledata = new JSONObject(displayConfig.get("datas").toString());
//			if (gettabledata.has("email")) {
//				email = new JSONObject(gettabledata.get("email").toString());
//				mail = new JSONArray(gettabledata.getJSONArray("mail"));
////  				mail=jsonheader.getString("email");
//			}
//			// primary key column name comes in table
//			String columnprimarykey = gettabledata.getJSONObject("primarykey").getString("columnname");
//
//			// filepathname was define which column value was set on file path
//			JSONArray jarr = new JSONArray(gettabledata.getJSONArray("filepathname").toString());
//
//			// set list of column name in json body (column name)
//			JSONArray column = new JSONArray(gettabledata.getJSONArray("column").toString());
//			String filename = "";
//			for (int i = 0; i < jarr.length(); i++) {
//				filename += jsonbody.getString(jarr.get(i).toString());
//			}
//			String filePath = "onboard/" + filename + dateFormat.format(new Date());
//			if (uploadFile(base64, filePath)) {
//				String path = DisplaySingleton.memoryApplicationSetting.get("s3url").toString() + filePath;
//				jsonbody.put(column.getString(0), path);
//			}
//
//			String url = "";
//			String res = "";
//			String response = "";
//			if (method.equalsIgnoreCase("POST")) {
//				url = pgresturl + gettabledata.getString("api");
//				response = dataTransmit.transmitDataspgrestpost(url, jsonbody.toString());
//			} else if (method.equalsIgnoreCase("PUT")) {
//				url = pgresturl + gettabledata.getString("api") + "?" + columnprimarykey + "=eq."
//						+ (jsonbody.get(columnprimarykey)).toString();
//				url = url.replace(" ", "%20");
//				response = dataTransmit.transmitDataspgrestput(url, jsonbody.toString());
//			}
//
//			if (response.equalsIgnoreCase("success")) {
//				returndata.put("reflex", "Success");
//			} else if (Integer.parseInt(response) >= 200 && Integer.parseInt(response) <= 226) {
//				if (gettabledata.has("email"))
//					emailconfig(mail, email, columnprimarykey, jsonbody, pgresturl);
//				else
//					returndata.put("reflex", "Success");
//			} else {
//				res = HttpStatus.getStatusText(Integer.parseInt(response));
//				returndata.put(ERROR, res);
//			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		return returndata.toString();

	}

	/// Stroe process
//	if (gettabledata.has("storedprocess") && isProcess) {
//	JSONObject storedprocess = new JSONObject(gettabledata.get("storedprocess").toString());
//	if (storedprocess.has("insert") && !storedprocess.getJSONObject("insert").isEmpty()) {
//		JSONObject json = new JSONObject();
//		JSONObject insertJSON = new JSONObject(storedprocess.get("insert").toString());
//		for (int i = 0; i < insertJSON.getJSONArray("params").length(); i++) {
//			if (!insertJSON.getJSONArray("columns").get(i).toString().equalsIgnoreCase("file")) {
//				String key = insertJSON.getJSONArray("params").getString(i);
//				json.put(key, jsonbody.getString(insertJSON.getJSONArray("columns").get(i).toString()));
//			} else {
//				json.put("docs", filejson.toString());
//			}
//		}
//		url = pgresturl + new JSONObject(storedprocess.get("update").toString()).getString("query");
//	} else {
//		url = pgresturl + gettabledata.getString("api");
//		url = url.replace(" ", "%20");
//		response = dataTransmit.transmitDataspgrestpost(url, jsonbody.toString(), false);
//	}
//} else {
}