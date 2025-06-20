package com.eit.abcdframework.serverbo;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.eit.abcdframework.globalhandler.GlobalAttributeHandler;
import com.eit.abcdframework.globalhandler.GlobalExceptionHandler;
import com.eit.abcdframework.http.caller.Httpclientcaller;
import com.eit.abcdframework.service.FormdataServiceImpl;
import com.eit.abcdframework.util.AmazonSMTPMail;
import com.eit.abcdframework.util.MessageServices;
import com.eit.abcdframework.util.TimeZoneServices;
import com.eit.abcdframework.websocket.WebSocketService;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class CommonServices {

//	@Value("${applicationurl}")
//	private String applicationurl;

	@Autowired
	Httpclientcaller dataTransmit;

	@Autowired
	WebSocketService socketService;

	@Autowired
	AmazonSMTPMail amazonSMTPMail;

	@Autowired
	MessageServices messageServices;

//	@Autowired
	static FormdataServiceImpl formdataServiceImpl;

	@Autowired
	public void setProductService(FormdataServiceImpl formdataServiceImpl) {
		CommonServices.formdataServiceImpl = formdataServiceImpl;
	}

	private static final Logger LOGGER = LoggerFactory.getLogger("CommonServices");

//	// Define characters allowed in the verification code
	private static final String ALLOWED_CHARACTERS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
//
//	// Define the maximum length of the verification code
	private static final int MAX_CODE_LENGTH = 6;
//
	private static final String ALGORITHM = "AES/CBC/PKCS5Padding"; // AES with CBC and PKCS5 padding
	private static final String SECRET_KEY = "ABCDFRAM09876543"; // 16-byte key for AES
	private static final String IV = "ABCDFRAMIV098765"; // 16-byte IV for AES

	@Value("${schema}")
	private String schema;

	public String userstatusupdate(String updationform, JSONObject user, String id) {
		String url = "";
		String response = "";
		String tablename = "";
		try {
			String sendby = "";
			JSONObject json = new JSONObject();
			if (updationform.equalsIgnoreCase("login")) {
				json.put("id", id);
				json.put("companyid", user.get("companyid"));
				json.put("username", user.getString("emailaddress"));
				json.put("companyname", user.getString("companyname"));
				json.put("logintime", TimeZoneServices.getDateInTimeZoneforSKT("Asia/Riyadh"));
				tablename = "logsofuser";
				sendby = "post";
			} else if (updationform.equalsIgnoreCase("logout")) {
				json.put("id", id);
				json.put("logouttime", TimeZoneServices.getDateInTimeZoneforSKT("Asia/Riyadh"));
				tablename = "logsofuser?id=eq." + id;
				sendby = "put";
			}
			url = GlobalAttributeHandler.getPgrestURL() + tablename;
			if (sendby.equalsIgnoreCase("post"))
				response = dataTransmit.transmitDataspgrestpost(url, json.toString(), false, schema);
			else
				response = dataTransmit.transmitDataspgrestput(url, json.toString(), false, schema);

//			if (Integer.parseInt(response) <= 200 || Integer.parseInt(response) >= 226) {

//			if(!new JSONObject((new JSONArray(response).get(0).toString())).has("reflex")) {
//				String res = HttpStatus.getStatusText(Integer.parseInt(response));
//				returndata.put("error", res);
//			} else {
//				returndata.put("reflex", "Success");
//			}

		} catch (Exception e) {
			LOGGER.error("Exception at User Status Update ::", e);
			return new JSONObject().put(GlobalExceptionHandler.getError(), e.getMessage()).toString();

		}
		return response;
	}

	public String userActivation(String key) {
		JSONObject returnMessage = new JSONObject();
		try {
			JSONObject getconfigofactivation = new JSONObject(
					DisplaySingleton.memoryApplicationSetting.get("useractivationConfig").toString());

			String url = GlobalAttributeHandler.getPgrestURL() + getconfigofactivation.getString("tablename") + "?"
					+ getconfigofactivation.getString("verificationcolumn") + "=eq." + key;
			JSONArray userData = dataTransmit.transmitDataspgrest(url, schema);
			if (!userData.isEmpty()) {
				JSONObject datas = new JSONObject(userData.get(0).toString());
				if (!datas.getBoolean("mailverification")) {
					datas.put("mailverification", true);
					url = GlobalAttributeHandler.getPgrestURL() + getconfigofactivation.getString("tablename") + "?"
							+ getconfigofactivation.getString("primarykey") + "=eq."
							+ datas.get(getconfigofactivation.getString("primarykey"));
					String result = dataTransmit.transmitDataspgrestput(url, datas.toString(), false, schema);
//					if (Integer.parseInt(result) >= 200 && Integer.parseInt(result) <= 226) {
					if (new JSONObject((new JSONArray(result).get(0).toString())).has("reflex")) {
						returnMessage.put("reflex", "Successfully Verified");
					}
				} else {
					return returnMessage.put("error", "This User Already Verified").toString();
				}
			} else {
				return returnMessage.put("error", "Verification Failed").toString();
			}
		} catch (Exception e) {
			returnMessage.put("error", "Verification Failed");
			LOGGER.error("Exception at " + Thread.currentThread().getStackTrace()[1].getMethodName(), e);
		}
		return returnMessage.toString();
	}

	public static String generateVerificationCode(int maxLength) {
		SecureRandom random = new SecureRandom();
		StringBuilder codeBuilder = new StringBuilder(maxLength);

		// Generate random characters for the code
		for (int i = 0; i < maxLength; i++) {
			int randomIndex = random.nextInt(ALLOWED_CHARACTERS.length());
			char randomChar = ALLOWED_CHARACTERS.charAt(randomIndex);
			codeBuilder.append(randomChar);
		}

		return codeBuilder.toString();
	}

	public Long generateVerificationOTP(String id) {
		String ALLOWED_CHARACTERS = "0123456789";
		Long OTP = null;
//		Long OTP = (long) Math.floor(1000 + Math.random() * 9000);
		try {
			JSONObject forgotJson = new JSONObject(
					DisplaySingleton.memoryApplicationSetting.get("forgotpassConfig").toString());
			int otplen = forgotJson.getInt("otplength");

			SecureRandom random = new SecureRandom();
			StringBuilder codeBuilder = new StringBuilder(otplen);

			// Generate random characters for the code
			for (int i = 0; i < otplen; i++) {
				int randomIndex = random.nextInt(ALLOWED_CHARACTERS.length());
				char randomChar = ALLOWED_CHARACTERS.charAt(randomIndex);
				codeBuilder.append(randomChar);
			}
			OTP = Long.parseLong(codeBuilder.toString());
			if (forgotJson.getBoolean("OTPStore")) {
				JSONObject jsonbody = new JSONObject();
				jsonbody.put("user_id", id);
				jsonbody.put("otp_code", OTP);
				String url = GlobalAttributeHandler.getPgrestURL() + "/otp_verification?user_id=eq." + id;
				JSONArray dataArray = dataTransmit.transmitDataspgrest(url, schema);
				if (dataArray.isEmpty()) {
					url = GlobalAttributeHandler.getPgrestURL() + "/otp_verification";
					dataTransmit.transmitDataspgrestpost(url, jsonbody.toString(), false, schema);
				} else {
//					JSONObject jsonData=new JSONObject(dataArray.get(0).toString());
//					if(jsonData.getInt("attempts")<3) {
//						if(jsonData.getString("created_at").equalsIgnoreCase(new Date))
//					}
					jsonbody.put("attempts", 0);
					jsonbody.put("id", new JSONObject(dataArray.get(0).toString()).get("id"));
					url = GlobalAttributeHandler.getPgrestURL() + "/otp_verification?id=eq."
							+ new JSONObject(dataArray.get(0).toString()).get("id");
					dataTransmit.transmitDataspgrestput(url, jsonbody.toString(), false, schema);
				}
			}
		} catch (Exception e) {
			LOGGER.error("Exception at " + Thread.currentThread().getStackTrace()[1].getMethodName(), e);
			
		}
		return OTP;

	}

	public String VerificationOTP(String OTP, String id) {
		JSONObject jsonBody = null;
		String res = "";
		try {
			String url = GlobalAttributeHandler.getPgrestURL() + "/otp_verification?user_id=eq." + id;
			JSONArray dataArray = dataTransmit.transmitDataspgrest(url, schema);
			if (!dataArray.isEmpty()) {
				jsonBody = new JSONObject(dataArray.get(0).toString());
				if (jsonBody.getInt("attempts") < 3) {
					if (jsonBody.getString("otp_code").equalsIgnoreCase(OTP)) {
						jsonBody.put("attempts", jsonBody.getInt("attempts") + 1);
						jsonBody.put("verified", true);
						url = GlobalAttributeHandler.getPgrestURL() + "/otp_verification?id=eq." + jsonBody.get("id");
						dataTransmit.transmitDataspgrestput(url, jsonBody.toString(), false, schema);
						res = "Verified";
					} else {
						jsonBody.put("attempts", jsonBody.getInt("attempts") + 1);
						res = "Retry Verification OTP Dose not Match";
						url = GlobalAttributeHandler.getPgrestURL() + "/otp_verification?id=eq." + jsonBody.get("id");
						dataTransmit.transmitDataspgrestput(url, jsonBody.toString(), false, schema);
					}
				} else {
					return "Too Many Attempt,So Please Retry After 24Hrs";
				}

			}

		} catch (Exception e) {
			LOGGER.error("Exception at " + Thread.currentThread().getStackTrace()[1].getMethodName(), e);
			return new JSONObject().put(GlobalExceptionHandler.getError(), e.getMessage()).toString();

		}

		return res;

	}

	public String sendMailForVerification(String mail, String mode, String lang) {
		JSONObject returndata = new JSONObject();
		String body = "";
		String subject = "";
		String code = "";
		try {
			if (mode.equalsIgnoreCase("link")) {

			} else if (mode.equalsIgnoreCase("otp")) {
				code = CommonServices.generateVerificationCode(MAX_CODE_LENGTH);
				body = new JSONObject(
						(new JSONObject(DisplaySingleton.memoryEmailCofig.get("otpverification").toString())
								.get("contenttype")).toString())
						.getString(lang).replaceAll("codeOTP", code);
			}
			subject = new JSONObject(DisplaySingleton.memoryEmailCofig.get("otpverification").toString())
					.getJSONObject("subject").getString(lang);
			amazonSMTPMail.sendEmail(mail, subject, body);

			returndata.put("reflex", Base64.getEncoder().encodeToString(code.getBytes()));
		} catch (Exception e) {
			LOGGER.error("Exception at " + Thread.currentThread().getStackTrace()[1].getMethodName(), e);
			returndata.put("error", "Faild Verification");
		}
		return returndata.toString();

	}

	public static String decrypt(String encryptedData) throws Exception {
		SecretKeySpec keySpec = new SecretKeySpec(SECRET_KEY.getBytes(), "AES");
		IvParameterSpec ivSpec = new IvParameterSpec(IV.getBytes());

		Cipher cipher = Cipher.getInstance(ALGORITHM);
		cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);

		byte[] decodedBytes = Base64.getDecoder().decode(encryptedData);
		byte[] decryptedBytes = cipher.doFinal(decodedBytes);

		return new String(decryptedBytes);
	}

	public Map<String, Object> loadBase64(String value, int total_pages) throws JSONException, IOException {
		String url = "";
		Map<String, Object> base64String = new HashMap<>();
		LOGGER.info("Enter into fetch a base64!!");

		if (total_pages <= 100) {
			url = GlobalAttributeHandler.getPgrestURL() + "pdf_splitter?select=document&primary_id_pdf=eq." + value;
			return new JSONObject(dataTransmit.transmitDataspgrest(url, schema).get(0).toString())
					.getJSONObject("document").toMap();

		} else if (total_pages > 100) {
			int start_page = 1;
			ExecutorService executorService = Executors.newFixedThreadPool(20);

			try {
				while (start_page < total_pages) {
					final int current_start_page = start_page;
					final int current_end_page = (start_page == 1 ? (start_page + 99)
							: (start_page + 100)) > total_pages ? total_pages
									: start_page == 1 ? (start_page + 99) : (start_page + 100);

					executorService.submit(() -> {

						String urls = GlobalAttributeHandler.getPgrestURL() + "rpc/get_pdf_splitdata?start_page="
								+ current_start_page + "&end_page=" + current_end_page + "&datas=primary_id_pdf='"
								+ value + "'";

						try {
							JSONObject jsonObject = new JSONObject(
									dataTransmit.transmitDataspgrest(urls, schema).get(0).toString())
									.getJSONObject("images");

							jsonObject.keys().forEachRemaining(key -> {
								synchronized (base64String) {
									base64String.put(key, jsonObject.getString(key));
								}
							});

							LOGGER.info("Successfully Put Into Object startPages: {} --- endPage: {}",
									current_start_page, current_end_page);

						} catch (Exception e) {
							LOGGER.error("Error processing pages {} to {}: {}", current_start_page, current_end_page,
									e.getMessage());
						}
					});
					start_page = current_end_page > total_pages ? total_pages : current_end_page;
				}
			} finally {
				executorService.shutdown();
				try {
					if (!executorService.awaitTermination(60, TimeUnit.MINUTES)) {
						executorService.shutdownNow();
					}
				} catch (InterruptedException e) {
					executorService.shutdownNow();
					Thread.currentThread().interrupt();
				}
			}
		}
		return base64String;

	}

	
//	public static String mappedCurdOperation(JSONObject getdataObject, String data, String response) {
//		String res = "";
//		try {
//			Map<String, Boolean> formDataResponces = new HashMap<>();
//			JSONArray methods = getdataObject.getJSONObject("synchronizedCurdOperation").getJSONArray("Methods");
//			JSONArray bodJson = getdataObject.getJSONObject("synchronizedCurdOperation").getJSONArray("bodyJson");
//
//			for (int method = 0; method < methods.length(); method++) {
//				String keyofmethods = methods.get(method).toString();
//
//				if (keyofmethods.equalsIgnoreCase("post")) {
//					res = formdataServiceImpl.transmittingToMethod("POST", data, bodJson.get(method).toString(),response);
//					formDataResponces.put(bodJson.get(method).toString(),
//							(new JSONObject(res).has("reflex") ? true : false));
//				} else if (keyofmethods.equalsIgnoreCase("put")) {
//					res = formdataServiceImpl.transmittingToMethod("PUT", data, bodJson.get(method).toString(),response);
//					formDataResponces.put(bodJson.get(method).toString(),
//							(new JSONObject(res).has("reflex") ? true : false));
//				} 
//				
//				Set<String> Failed = formDataResponces.entrySet().stream().filter(entry -> !entry.getValue())
//						.map(Map.Entry::getKey).collect(Collectors.toSet());
//				res = Failed.isEmpty() ? "Success" : "Missed Api" + Failed;
//				LOGGER.error(Thread.currentThread().getStackTrace()[0].getMethodName() + "{}-->{}", bodJson.get(method),
//						res);
//
//			}
//
//		} catch (Exception e) {
//			LOGGER.error(Thread.currentThread().getStackTrace()[0].getMethodName(), e);
//		}
//
//		return res;
//	}
	
	public static String mappedCurdOperation2(JSONObject getdataObject, String data, String response) {
		String res = "";
		try {
			Map<String, Boolean> formDataResponces = new HashMap<>();
			JSONObject payload = new JSONObject(data);
//			JSONArray methods = getdataObject.getJSONObject("synchronizedCurdOperation").getJSONArray("Methods");
			JSONArray bodJson = getdataObject.getJSONObject("synchronizedCurdOperation").getJSONArray("bodyJson");
			for(int i=0;i<bodJson.length();i++) {
					String bJson = bodJson.get(i).toString();
				if (payload.has(bJson) && (payload.get(bJson) instanceof JSONObject)) {
				    
					JSONObject jsonheader = payload.getJSONObject(bJson).getJSONObject("header");
				String method = jsonheader.getString("method");
				if(method.equalsIgnoreCase("post")) {
					res = formdataServiceImpl.transmittingToMethod("POST", data, bodJson.get(i).toString(),response);
					formDataResponces.put(bodJson.get(i).toString(),
							(new JSONObject(res).has("reflex") ? true : false));
				}else if(method.equalsIgnoreCase("put")) {
					res = formdataServiceImpl.transmittingToMethod("PUT", data, bodJson.get(i).toString(),response);
					formDataResponces.put(bodJson.get(i).toString(),
							(new JSONObject(res).has("reflex") ? true : false));
				}
			}
	
	

				Set<String> Failed = formDataResponces.entrySet().stream().filter(entry -> !entry.getValue())
						.map(Map.Entry::getKey).collect(Collectors.toSet());
				res = Failed.isEmpty() ? "Success" : "Missed Api" + Failed;
				LOGGER.error(Thread.currentThread().getStackTrace()[0].getMethodName() + "{}-->{}", bodJson.get(i),
						res);

			}

		} catch (Exception e) {
			LOGGER.error(Thread.currentThread().getStackTrace()[0].getMethodName(), e);
		}

		return res;
	}
	
	@Async
	public CompletableFuture<String> mappedCurdOperationASYNC(JSONObject getdataObject, String data,String response) {
		String result = "";
		try {
			Map<String, Boolean> formDataResponces = new HashMap<>();
			JSONArray methods = getdataObject.getJSONObject("asyncCurdOperation").getJSONArray("Methods");
			JSONArray bodJson = getdataObject.getJSONObject("asyncCurdOperation").getJSONArray("bodyJson");
			for (int method = 0; method < methods.length(); method++) {
				String keyofmethods = methods.get(method).toString();
				if (keyofmethods.equalsIgnoreCase("post")) {
					result = formdataServiceImpl.transmittingToMethod("POST", data, bodJson.get(method).toString(),response);
					formDataResponces.put(bodJson.get(method).toString(),
							(new JSONObject(result).has("reflex") ? true : false));
				} else if (keyofmethods.equalsIgnoreCase("put")) {
					result = formdataServiceImpl.transmittingToMethod("PUT", data, bodJson.get(method).toString(),response);
					formDataResponces.put(bodJson.get(method).toString(),
							(new JSONObject(result).has("reflex") ? true : false));
				} else {

				}

				Set<String> Failed = formDataResponces.entrySet().stream().filter(entry -> !entry.getValue())
						.map(Map.Entry::getKey).collect(Collectors.toSet());
				result = Failed.isEmpty() ? "Success" : "Missed Api" + Failed;
				LOGGER.error(Thread.currentThread().getStackTrace()[0].getMethodName() + "{}-->{}", bodJson.get(method),
						result);

			}

		} catch (Exception e) {
			LOGGER.error(Thread.currentThread().getStackTrace()[0].getMethodName(), e);
		}

		return CompletableFuture.completedFuture(result);
	}

	public String whereFormation(JSONObject jsonbody, JSONObject whereFormation) {
		String whereCondition = "";
		try {
			JSONArray arrayofwhere = whereFormation.getJSONArray("where");

			for (int i = 0; i < arrayofwhere.length(); i++) {
				if (i == 0) {
					whereCondition = "?" + arrayofwhere.get(i).toString() + "=eq.";
					String getby = whereFormation.getJSONArray("value").get(i).toString().split("-")[0];
					if (getby.equalsIgnoreCase("param")) {
						whereCondition += whereFormation.getJSONArray("value").get(i).toString().split("-")[1];
					} else if (getby.equalsIgnoreCase("body")) {
						System.err.println(whereFormation.getJSONArray("value").get(i).toString());
						whereCondition += jsonbody
								.get(whereFormation.getJSONArray("value").get(i).toString().split("-")[1]);

					}
				} else {
					whereCondition = "&" + arrayofwhere.get(i).toString() + "=eq.";
					String getby = whereFormation.getJSONArray("value").get(i).toString().split("-")[0];
					if (getby.equalsIgnoreCase("param")) {
						whereCondition += whereFormation.getJSONArray("value").get(i).toString().split("-")[1];
					} else if (getby.equalsIgnoreCase("body")) {
						whereCondition += jsonbody
								.get(whereFormation.getJSONArray("value").get(i).toString().split("-")[1]);
					}

				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return whereCondition;
	}

	public static String urlFormation(JSONObject formationData, JSONObject body) {
		String url = "";
		try {
			List<String> where = new ArrayList<>();
			if (formationData.getString("formationtype").equalsIgnoreCase("pgrest")) {

				JSONObject formation = formationData.getJSONObject("pgrest");
				List<Object> list = (List<Object>) formation.getJSONObject("param").keys();

				IntStream.range(0, list.size()).forEach(i -> {
					if (where.isEmpty())
						where.add("?" + list.get(i) + "=" + formation.getJSONArray("operator").get(i)
								+ body.getString(formation.getJSONObject("param").getString(list.get(i).toString())));
					else
						where.add("&" + list.get(i) + "=" + formation.getJSONArray("operator").get(i)
								+ body.getString(formation.getJSONObject("param").getString(list.get(i).toString())));

				});
				url = (url + formation.getString("api") + where.stream().collect(Collectors.joining("")))
						.replaceAll(" ", "%20");
			} else if (formationData.getString("formationtype").equalsIgnoreCase("function")) {
				JSONObject formation = formationData.getJSONObject("function");

				if (!formation.getJSONObject("param").isEmpty()) {
					List<Object> list = (List<Object>) formation.getJSONObject("param").keys();
					list.forEach(entry -> {
						if (where.isEmpty())
							where.add("?" + entry + "="
									+ body.getString(formation.getJSONObject("param").getString(entry.toString())));
						else
							where.add("&" + entry + "="
									+ body.getString(formation.getJSONObject("param").getString(entry.toString())));

					});
					url = (url + formation.getString("api") + where.stream().collect(Collectors.joining("")))
							.replaceAll(" ", "%20");
				}
			}

		} catch (Exception e) {
			LOGGER.error(Thread.currentThread().getStackTrace()[0].getMethodName(), e);
		}

		return url;

	}

	public void addAdditionalFields(JSONObject datas, JSONArray res, JSONObject jqxdetails, boolean isConvert,
			JSONObject additionalInformation) {
		try {
			if (additionalInformation == null && !isConvert)
				return;

			JSONArray inputColumns = jqxdetails.getJSONArray("columns");
			JSONArray defaults = jqxdetails.getJSONObject("showgridbyrole").getJSONArray("default");
			JSONObject discfgOfAF = null;
			JSONObject datasOfAF = null;
			List<Object> fieldsToAdd = new ArrayList<Object>();
			HashMap<String, JSONObject> additionalResMap = new HashMap<String, JSONObject>();
			;
			HashMap<String, JSONObject> additionalJqxColumns = new HashMap<String, JSONObject>();

			if (additionalInformation != null && !additionalInformation.toString().equals("{}")) {

				JSONObject configs = DisplaySingleton.memoryDispObjs2.getJSONObject("additionalInformation");
				discfgOfAF = new JSONObject(configs.get("discfg").toString());
				datasOfAF = new JSONObject(configs.get("datas").toString());

				String url = GlobalAttributeHandler.getPgrestURL();
				url += additionalInformation.getBoolean("additionalFunction") ? datasOfAF.getString("Function")
						: datasOfAF.getString("GET");
				url += "?basequery=";
				JSONObject param = new JSONObject();
				param.put("query", datasOfAF.getJSONObject("Query").getString("query"));
				param.put("where", "where " + additionalInformation.optString("additionalWhere"));
				url += param;
				LOGGER.info("URL for additional : " + url);
				//LOGGER.info("get 0 : "+dataTransmit.transmitDataspgrest(url, datasOfAF.getString("schema")).toString());	
				JSONArray additionalRes =
						dataTransmit.transmitDataspgrest(url, datasOfAF.getString("schema"));

				fieldsToAdd = additionalInformation.optJSONArray("additionalFields").toList(); // Fields to be added in
																								// the final res

				for (int i = 0; i < additionalRes.length(); i++) { // converting additioanlres into map for performance
					additionalResMap.put(additionalRes.getJSONObject(i).getString(datas.getString("uniqueColumn")),
							additionalRes.getJSONObject(i));
				}

				JSONArray additionalColumns = discfgOfAF.getJSONObject("jqxdetails").getJSONArray("columns"); // getting
																												// columns
																												// from
																												// discfg
				for (int i = 0; i < additionalColumns.length(); i++) { // converting into map for peformace
					additionalJqxColumns.put(additionalColumns.getJSONObject(i).getString("columnname"),
							additionalColumns.getJSONObject(i));
				}

				for (String key : additionalJqxColumns.keySet()) { // adding column jsons for additional columns
					if (fieldsToAdd.contains(key)) {
						inputColumns.put(additionalJqxColumns.get(key));
						defaults.put(key);
					}
				}

			}

			JSONArray timeFields = datas.optJSONArray("TIMEFIELDS"); // time fields to change format

			for (int i = 0; i < res.length(); i++) { // Iterating each JSON in res
				JSONObject row = res.getJSONObject(i);
				if (additionalInformation != null && !additionalInformation.toString().equals("{}")) {

					JSONObject add = additionalResMap.get(row.getString(datas.getString("uniqueColumn")));
					if (add == null) {
						for (Object o : fieldsToAdd) {
							String s = o.toString();
							row.put(s, "");
						}
					} else {
						for (String key : add.keySet()) {
							if (fieldsToAdd.contains(key)) {
								row.put(key, add.get(key));
							}
						}
					}
				}
				if (isConvert && timeFields != null) {
					for (Object o : timeFields) {
						String s = o.toString();
						row.put(s, convertTo12HrFormat(row.get(s).equals(null) ? null : row.getString(s)));
					}
				}
			}

		} catch (Exception e) {
			LOGGER.error(Thread.currentThread().getStackTrace()[0].getMethodName(), e);
		}
	}

	public static String convertTo12HrFormat(String time24) {
		try {
			if (time24.contains(" ")) {
				return TimeZoneServices.get24to12(time24, "date&time");
			} else {
				return TimeZoneServices.get24to12(time24, "time");
			}
		} catch (Exception e) {
			LOGGER.error(Thread.currentThread().getStackTrace()[0].getMethodName(), e);
			return time24;
		}
	}

	public void changeGeoCodeToAddress(JSONArray res, JSONObject datas) {
		try {

			List<Object> latLongColumns = datas.optJSONArray("latLongColumns") != null
					? datas.optJSONArray("latLongColumns").toList()
					: new JSONArray().toList();
			LOGGER.info("latlongColumsns : " + latLongColumns.toString());
			IntStream.range(0, res.length()).mapToObj(res::getJSONObject).forEach(obj -> {
				latLongColumns.stream().forEach(latLong -> {
					String latlng = obj.getString(latLong.toString());
					if (DisplaySingleton.addressCache.has(latlng)) {
						obj.put(latLong.toString(), DisplaySingleton.addressCache.get(latlng));
					} else {
						String address = getAddressFromLatLng(latlng.split(",")[0], latlng.split(",")[1], "");
						DisplaySingleton.addressCache.put(latlng, address);
						obj.put(latLong.toString(), address);
					}
				});
			});
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public String getAddressFromLatLng(String lat, String lon, String language) {

		try {
			String url = "https://nominatim.openstreetmap.org/reverse?format=json&lat=" + lat + "&lon=" + lon;
			JSONObject header = new JSONObject();
			header.put("Accept-Language", (language.equalsIgnoreCase("") ? "en" : language));
			JSONObject jsonObject = new JSONObject(
					dataTransmit.transmitDatas(url, header, "GET").get(0).toString());

			String displayName = jsonObject.optString("display_name", lat + "," + lon);
			return displayName;
		} catch (Exception e) {
			e.printStackTrace();
			return new JSONObject().put(GlobalExceptionHandler.getError(), e.getMessage()).toString();
		}
	}
	
	public static String getOrderedJSONObject(JSONObject input) {
		 String[] keysInOrder = { "query", "where", "groupby", "having", "orderby", "limit" };
		    LinkedHashMap<String, Object> orderedMap = new LinkedHashMap<>();

		    for (String key : keysInOrder) {
		        if (input.has(key)) {
		            orderedMap.put(key, input.get(key));
		        }
		    }
		    try {
		        ObjectMapper objectMapper = new ObjectMapper();
		        String orderedJson = objectMapper.writeValueAsString(orderedMap);
		        return orderedJson;
		    } catch (Exception e) {
		        e.printStackTrace();
		        return null;
		    }
	 }
	
	public static String  alterConfFile(String newSchemaName) {
		String filePath = "/usr/local/postgrest/postgrest.conf";
		StringBuilder content = new StringBuilder();
		try(BufferedReader br = new BufferedReader(new FileReader(filePath))){
			String line;
			while((line = br.readLine()) != null) {
				if(line.startsWith("db-schemas")) {
					String schemasListed = line.split("=")[1].replaceAll("\"","");
					String[] schemasList = schemasListed.split(",");
					for(String s : schemasList) {
						if(s.trim().equals(newSchemaName)) {
							LOGGER.error("Schema "+newSchemaName+" already exist in conf file ....");
							return "Failed";
						}
					}
					
					LOGGER.info("Schemas Before : "+schemasListed);
					schemasListed = newSchemaName+","+schemasListed;
					LOGGER.info("Schemas After : "+schemasListed);
					StringBuilder updatedLine = new StringBuilder().append("db-schemas = ")
												.append("\"").append(schemasListed).append("\"");
					content.append(updatedLine.toString());
					content.append("\n");
					
				}else {
				content.append(line);
				content.append("\n");
				}
			}
		}
		catch(Exception e) {
			e.printStackTrace();
			return "Failed when reading and altering";
		}
		try(BufferedWriter bw = new BufferedWriter(new FileWriter(filePath))){
			
			bw.write(content.toString());
			bw.flush();
			
			if(restartPostgrestWithCheck()) {
			return "Successfully Altered and Restarted Postgrest server";
			}
			else {
				return "Failed when restarting postgrest";
			}
			
		}catch(Exception e) {
			e.printStackTrace();
			return "Failed when writing";
		}
	}
	
	public static Boolean restartPostgrestWithCheck() {
	    try {
	        
	        String killCmd = "kill $(ps aux | grep '[p]ostgrest postgrest.conf' | awk '{print $2}')";
	        Process killProcess = Runtime.getRuntime().exec(new String[]{"/bin/sh", "-c", killCmd});
	        killProcess.waitFor();
	        Thread.sleep(1000); 

	       
	        String startCmd = "cd /usr/local/postgrest && nohup ./postgrest ./postgrest.conf > postgrest.log 2>&1 &";
	        Process startProcess = Runtime.getRuntime().exec(new String[]{"/bin/sh", "-c", startCmd});
	        startProcess.waitFor();
	        Thread.sleep(2000); 

	        
	        String checkCmd = "ps aux | grep '[p]ostgrest postgrest.conf'";
	        Process checkProcess = Runtime.getRuntime().exec(new String[]{"/bin/sh", "-c", checkCmd});
	        BufferedReader reader = new BufferedReader(new InputStreamReader(checkProcess.getInputStream()));

	        String line;
	        boolean running = false;
	        while ((line = reader.readLine()) != null) {
	            if (line.contains("postgrest.conf")) {
	                running = true;
	                break;
	            }
	        }

	        checkProcess.waitFor();

	        if (running) {
	            return true;
	        } else {
	            return false;
	        }

	    } catch (Exception e) {
	        e.printStackTrace();
	        return false;
	    }
	}

	

}
