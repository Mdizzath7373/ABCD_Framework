package com.eit.abcdframework.service;

import java.io.File;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.eit.abcdframework.dto.CommonUtilDto;
import com.eit.abcdframework.globalhandler.GlobalAttributeHandler;
import com.eit.abcdframework.http.caller.Httpclientcaller;
import com.eit.abcdframework.serverbo.CommonServices;
import com.eit.abcdframework.serverbo.DisplayConfigBO;
import com.eit.abcdframework.serverbo.DisplayHandler;
import com.eit.abcdframework.serverbo.DisplaySingleton;
import com.eit.abcdframework.serverbo.FileuploadServices;
import com.eit.abcdframework.serverbo.ResponcesHandling;
import com.eit.abcdframework.util.TimeZoneServices;
import com.eit.abcdframework.websocket.WebSocketService;

@Service
public class DCDesignDataServiceImpl implements DCDesignDataService {

	@Autowired
	DisplayConfigBO displayConfigBO;

	@Autowired
	Httpclientcaller dataTransmit;

	@Autowired
	FileuploadServices fileuploadServices;

	@Autowired
	WebSocketService socketService;

	@Autowired
	DisplayHandler displayHandler;

	@Autowired
	ResponcesHandling responcesHandling;

	@Autowired
	CommonServices commonServices;

	private static final Logger LOGGER = LoggerFactory.getLogger("DCDesignDataServiceImpl");

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

			// Body Structure two types
			JSONObject jsonheader = jsonObject1.has("PrimaryBody")
					? new JSONObject(jsonObject1.getJSONObject("PrimaryBody").getJSONObject("header").toString())
					: new JSONObject(jsonObject1.getJSONObject("header").toString());

			JSONObject jsonbody = jsonObject1.has("PrimaryBody")
					? new JSONObject(jsonObject1.getJSONObject("PrimaryBody").getJSONObject("body").toString())
					: new JSONObject(jsonObject1.getJSONObject("body").toString());

			if (jsonObject1.has("PrimaryBody")
					&& new JSONObject(jsonObject1.getJSONObject("PrimaryBody")).has("documents")) {
				documentdata = new JSONObject(
						jsonObject1.getJSONObject("PrimaryBody").getJSONObject("documents").toString());
			} else if (jsonObject1.has("documents")) {
				documentdata = new JSONObject(jsonObject1.getJSONObject("documents").toString());
			} else {
				documentdata = new JSONObject();
			}

			String displayAlias = jsonheader.getString("name");
			String method = jsonheader.getString("method");

			displayConfig = DisplaySingleton.memoryDispObjs2.getJSONObject(displayAlias);
			JSONObject gettabledata = new JSONObject(displayConfig.get("datas").toString());

			boolean isprogress = false;

			
			List<File> filedata = new ArrayList<>();

			if (transmitMethod.equalsIgnoreCase("Upload")) {
				fileuploadServices.fileupload(gettabledata, files, jsonbody, documentdata);
				for (MultipartFile mfile : files) {
					try {
						File file = Files.createTempFile(null, mfile.getOriginalFilename()).toFile();
						mfile.transferTo(file);
						filedata.add(file);
					} catch (Exception e) {
						LOGGER.error(Thread.currentThread().getStackTrace()[0].getMethodName(), e);
					}
				}
				
			} else if (transmitMethod.equalsIgnoreCase("UploadWithProgress")) {
				for (MultipartFile mfile : files) {
					try {
						File file = Files.createTempFile(null, mfile.getOriginalFilename()).toFile();
						mfile.transferTo(file);
						filedata.add(file);
					} catch (Exception e) {
						LOGGER.error(Thread.currentThread().getStackTrace()[0].getMethodName(), e);
					}
				}
				res = fileuploadServices.convertPdfToMultipart(filedata.get(0), gettabledata, jsonbody);
				isprogress = true;
				if (res.equalsIgnoreCase("Failed")) {
					new JSONObject().put(GlobalAttributeHandler.getError(), GlobalAttributeHandler.getFailure())
							.toString();
				}
			} else if (transmitMethod.equalsIgnoreCase("MergeFile")) {
				for (MultipartFile mfile : files) {
					try {
						File file = Files.createTempFile(null, mfile.getOriginalFilename()).toFile();
						mfile.transferTo(file);
						filedata.add(file);
					} catch (Exception e) {
						LOGGER.error(Thread.currentThread().getStackTrace()[0].getMethodName(), e);
					}
				}
				fileuploadServices.mergebase64ToPDF(gettabledata, jsonbody, filedata.get(0));
			}

			toSaveObject(method, jsonbody, gettabledata, jsonheader, filedata);

			if (isprogress) {
				Map<String, AtomicInteger> progress = fileuploadServices.getProgress();
				progress.put(jsonbody.get("ids").toString() + "-" + (jsonbody
						.get(gettabledata.getJSONObject("Splitter").getString("Splitter_primary_id")).toString()),
						new AtomicInteger(100));
				fileuploadServices.setProgress(progress);

				socketService.pushSocketData(jsonheader, jsonbody, "progress");
			}

			if (primary_id != 0) {
				String delUrl = GlobalAttributeHandler.getPgrestURL() + "pdf_splitter?id=eq." + primary_id;
				dataTransmit.transmitDataspgrestDel(delUrl, gettabledata.getString("schema"));
			} else {
				new JSONObject().put(GlobalAttributeHandler.getError(), GlobalAttributeHandler.getFailure()).toString();
			}

			if (gettabledata.has("asynOperation")) {
				LOGGER.info("Enter into Async Curd Opertion!");
				JSONArray typeOfMehods = gettabledata.getJSONObject("asynOperation").getJSONArray("type");
				for (int typeOfMehod = 0; typeOfMehod < typeOfMehods.length(); typeOfMehod++) {
					CompletableFuture<String> councurrentAPIres = new CompletableFuture<String>();
					if (typeOfMehods.get(typeOfMehod).toString().equalsIgnoreCase("Map")) {
						councurrentAPIres = commonServices.mappedCurdOperationASYNC(gettabledata, data);
						LOGGER.info("Councurrent API Response----->{}", councurrentAPIres);
					}
					if (!councurrentAPIres.isDone()) {
						return new JSONObject()
								.put(GlobalAttributeHandler.getError(), GlobalAttributeHandler.getFailure()).toString();
					}
				}
				LOGGER.info("Finish the Async Curd Opertion!");
			}

			if (gettabledata.has("synchronizedCurdOperation")) {
				LOGGER.info("Enter into synchronized Curd Opertion!");
				JSONArray typeOfMehods = gettabledata.getJSONObject("synchronizedCurdOperation").getJSONArray("type");
				for (int typeOfMehod = 0; typeOfMehod < typeOfMehods.length(); typeOfMehod++) {
					String councurrentAPIres = "";
					if (typeOfMehods.get(typeOfMehod).toString().equalsIgnoreCase("Map")) {
						councurrentAPIres = CommonServices.mappedCurdOperation(gettabledata, data);
						LOGGER.info("Councurrent API Response----->{}", councurrentAPIres);
					}
					if (!councurrentAPIres.equalsIgnoreCase("Success")) {
						return new JSONObject()
								.put(GlobalAttributeHandler.getError(), GlobalAttributeHandler.getFailure()).toString();
					}
				}
				LOGGER.info("Finish the synchronized Curd Opertion!");
			}

		} catch (

		Exception e) {
			LOGGER.error(Thread.currentThread().getStackTrace()[0].getMethodName(), e);
		}
		return new JSONObject().put(GlobalAttributeHandler.getReflex(), GlobalAttributeHandler.getSuccess()).toString();
	}

	private String toSaveObject(String method, JSONObject jsonbody, JSONObject gettabledata, JSONObject jsonheader,
			List<File> files) {
		JSONObject bodyData = null;
		try {
			String response = "";
			String url = "";
			String columnprimarykey = gettabledata.getJSONObject(GlobalAttributeHandler.getKey())
					.getString(GlobalAttributeHandler.getPrimarycolumnkey());

			if (gettabledata.has("expectedColumn")) {
				bodyData = new JSONObject(jsonbody.toString());
				jsonbody.remove(gettabledata.getString("expectedColumn"));

			} else {
				bodyData = new JSONObject(jsonbody.toString());
			}

			if (method.equalsIgnoreCase("POST")) {
				if (gettabledata.has("dateandtime")) {
					jsonbody.put(gettabledata.getString("dateandtime"),
							TimeZoneServices.getDateInTimeZoneforSKT("Asia/Riyadh"));
				}
				url = GlobalAttributeHandler.getPgrestURL() + gettabledata.getString("api").replaceAll(" ", "%20");
				response = dataTransmit.transmitDataspgrestpost(url, jsonbody.toString(), false,
						gettabledata.getString("schema"));
			} else if (method.equalsIgnoreCase("PUT")) {
				if (jsonbody.has(columnprimarykey) && !jsonbody.get(columnprimarykey).toString().equalsIgnoreCase("")) {
					// if use put method we need primary key (set primary key column name)
					url = (GlobalAttributeHandler.getPgrestURL() + gettabledata.getString("api") + "?"
							+ columnprimarykey + "=eq." + (jsonbody.get(columnprimarykey)).toString())
							.replaceAll(" ", "%20");
					response = dataTransmit.transmitDataspgrestput(url, jsonbody.toString(), false,
							gettabledata.getString("schema"));
				} else {
					return new JSONObject()
							.put(GlobalAttributeHandler.getError(), "primaryKey is Missing,Please Check this")
							.toString();
				}
			}

			responcesHandling.curdMethodResponceHandle(response, bodyData, jsonheader, gettabledata, method, files);
			LOGGER.info("Enter into Responce handle to Async Process");

		} catch (Exception e) {
			LOGGER.error(Thread.currentThread().getStackTrace()[0].getMethodName(), e);
		}
		return "";
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
					JSONObject quryJson = extraDatas.getJSONObject("Query");
					if (quryJson.has("where")) {
						String whereCon = quryJson.getString("where")
								+ (where.equalsIgnoreCase("") ? "" : " and " + where);
						quryJson.put("where", whereCon);
					}

					url = GlobalAttributeHandler.getPgrestURL() + extraDatas.getString("Function") + "?basequery="
							+ quryJson;
				} else {
					url = GlobalAttributeHandler.getPgrestURL() + extraDatas.getString(method);
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
					JSONObject quryJson = extraDatas.getJSONObject("Query");
					if (quryJson.has("where")) {
						String whereCon = quryJson.getString("where")
								+ (where.equalsIgnoreCase("") ? "" : " and " + where.replace("?datas=", ""));
						quryJson.put("where", whereCon);
					}

					url = GlobalAttributeHandler.getPgrestURL() + extraDatas.getString("Function") + "?basequery="
							+ quryJson;

					datavalues = new JSONObject(new JSONArray(
							dataTransmit.transmitDataspgrest(url, extraDatas.getString("schema")).get(0).toString()))
							.getJSONArray("datavalues");
				} else {
					datavalues = dataTransmit.transmitDataspgrest(url, extraDatas.getString("schema"));
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
			returnJson.put(GlobalAttributeHandler.getError(), "Something went worng, Please Retry");
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

	@Override
	public String SplitterPDFChanges(JSONObject jsonObject1) {
		JSONObject res = new JSONObject();
		try {
			JSONObject setValues = new JSONObject();

			JSONObject jsonHeader = new JSONObject(jsonObject1.get("header").toString());

			JSONObject jsonObject = new JSONObject(jsonObject1.get("body").toString());

			JSONObject docObj = jsonObject.getJSONObject("document");

			JSONObject displayConfig = DisplaySingleton.memoryDispObjs2.getJSONObject(jsonHeader.getString("name"));
			JSONObject gettabledata = new JSONObject(displayConfig.get("datas").toString());

			docObj.keys().forEachRemaining(key -> {
				String url = GlobalAttributeHandler.getPgrestURL() + "rpc/update_base64";
				JSONObject jsondata = new JSONObject();
				jsondata.put("key", key);
				jsondata.put("datavalue", docObj.getString(key));
				jsondata.put("primary", jsonObject.get("id"));

				setValues.put("datas", jsondata);

				dataTransmit.transmitDataspgrestpost(url, setValues.toString(), false,
						gettabledata.getString("schema"));

			});
			res.put(GlobalAttributeHandler.getReflex(), GlobalAttributeHandler.getSuccess());

		} catch (Exception e) {
			LOGGER.error(Thread.currentThread().getStackTrace()[0].getMethodName(), e);
			return new JSONObject().put(GlobalAttributeHandler.getError(), "Failed Please Retry").toString();
		}

		return res.toString();

	}

	@Override
	public String uploadImageProgress(List<MultipartFile> files, String data) {

		ExecutorService executorService = new ThreadPoolExecutor(30, 50, 60L, TimeUnit.SECONDS,
				new LinkedBlockingQueue<Runnable>(), new ThreadPoolExecutor.CallerRunsPolicy());
		try {

			List<Future<Boolean>> futures = new ArrayList<>();
			JSONObject S3urls = new JSONObject();
			for (int pageIndex = 0; pageIndex < files.size(); pageIndex++) {
				int currentIndex = pageIndex;
				futures.add(executorService.submit(() -> fileuploadServices.uploadfile(files.get(currentIndex),
						currentIndex, S3urls, new JSONObject(data))));
			}

			for (Future<Boolean> future : futures) {
				try {
					future.get();
				} catch (Exception e) {
					LOGGER.error(Thread.currentThread().getStackTrace()[0].getMethodName(), e);
				}
			}

			return S3urls.toString();

		} finally {
			// Shutdown the executor service
			LOGGER.info("Shutting down the thread pool!");
			executorService.shutdown();
			try {
				// Wait for tasks to complete before terminating the thread pool
				if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
					executorService.shutdownNow(); // Force shutdown if tasks exceed time limit
				}
			} catch (InterruptedException ex) {
				executorService.shutdownNow();
				Thread.currentThread().interrupt(); // Preserve interrupt status
				return "Failed";
			}
		}
	}

}