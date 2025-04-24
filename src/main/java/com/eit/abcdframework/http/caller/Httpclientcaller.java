package com.eit.abcdframework.http.caller;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.apache.hc.client5.http.classic.methods.HttpDelete;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpPut;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.util.Timeout;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.eit.abcdframework.config.ConfigurationFile;
import com.eit.abcdframework.serverbo.DisplayHandler;
import com.google.auth.oauth2.GoogleCredentials;

@Component("Httpclientcaller")
public class Httpclientcaller {

	public static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(DisplayHandler.class);

	private final CloseableHttpClient httpClient;

	public Httpclientcaller() {
		PoolingHttpClientConnectionManager poolingManager = new PoolingHttpClientConnectionManager();
		poolingManager.setMaxTotal(250);
		poolingManager.setDefaultMaxPerRoute(20);

		this.httpClient = HttpClients.custom().setConnectionManager(poolingManager)
				.setDefaultRequestConfig(RequestConfig.custom().setConnectionRequestTimeout(Timeout.ofSeconds(10))
						.setResponseTimeout(Timeout.ofSeconds(30)).build())
				.build();
	}

	public JSONArray executeRequest(HttpUriRequestBase request, String method) throws IOException {

		return httpClient.execute(request, response -> {
			return parseResponseBody(response, method);

		});

	}

	private JSONArray parseResponseBody(ClassicHttpResponse response, String method) {
		JSONArray responseArray = new JSONArray();
		try {
			int statusCode = response.getCode();
			LOGGER.info("Status Code is ::{}", statusCode);

			HttpEntity responseEntity = response.getEntity();
			if (responseEntity == null) {
				if (method.equalsIgnoreCase("get"))
					return responseArray;
				else {
					if (statusCode >= 200 && statusCode <= 226) {
						return responseArray.put(new JSONObject().put("reflex", "Successfully Verified"));
					} else {
						return responseArray.put(new JSONObject().put("error", "Failed"));
					}
				}
			}

			String responseBody = EntityUtils.toString(responseEntity);

			if (responseBody.isBlank() || responseBody.equals("{}") || responseBody.equals("[]")
					|| responseBody.equals("")) {
				if (method.equalsIgnoreCase("get"))
					return responseArray;
				else {
					if (statusCode >= 200 && statusCode <= 226) {
						return responseArray.put(new JSONObject().put("reflex", "Successfully Verified"));
					} else {
						return responseArray.put(new JSONObject().put("error", "Failed"));
					}
				}
			}

			if (responseBody.trim().startsWith("{")) {
				JSONObject jsonObject = new JSONObject(responseBody);
				if (jsonObject.has("reflex")) {
					return new JSONArray().put(jsonObject.getString("reflex"));
				}
				return responseArray.put(jsonObject);
			}

			if (responseBody.trim().startsWith("[")) {
				responseArray = new JSONArray(responseBody);
				if (responseArray.isEmpty()) {
					return responseArray;
				}

				if (responseArray.getJSONObject(0).has("reflex")) {
					return responseArray.put(responseArray.getJSONObject(0).getString("reflex"));
				}
				if (responseArray.getJSONObject(0).has("datavalues")
						&& responseArray.getJSONObject(0).get("datavalues").equals(null)) {
					return new JSONArray();
				} else {
					return new JSONObject(responseArray.get(0).toString()).getJSONArray("datavalues");
				}

			} else {
				responseArray.put(statusCode);
			}
		} catch (Exception e) {
			LOGGER.error("Error in {}: {}", Thread.currentThread().getStackTrace()[0].getMethodName(), e.getMessage());
		}
		return responseArray;
	}

	public JSONArray transmitDatas(String url, JSONObject header, String method) {
		try {
			HttpUriRequestBase request;

			// Create appropriate request based on method
			switch (method.toUpperCase()) {
			case "GET":
				request = new HttpGet(URLEncode(url).toString());
				break;
			case "POST":
				request = new HttpPost(url);
				break;
			case "PUT":
				request = new HttpPut(url);
				break;
			case "DELETE":
				request = new HttpDelete(url);
				break;
			default:
				LOGGER.error("Unsupported HTTP method: {}", method);
				JSONArray errorArray = new JSONArray();
				errorArray.put(new JSONObject().put("error", "Unsupported HTTP method: " + method));
				return errorArray;
			}

			// Set headers
			if (header != null) {
				Iterator<String> keys = header.keys();
				while (keys.hasNext()) {
					String key = keys.next();
					String value = header.optString(key);
					request.setHeader(key, value);
				}
			}

			// Set default headers if not present
			if (!request.containsHeader("Content-Type")) {
				request.setHeader("Content-Type", "application/json");
			}
			if (!request.containsHeader("Accept")) {
				request.setHeader("Accept", "application/json");
			}

			// Execute request using the existing method
			return executeRequest(request, method);

		} catch (Exception e) {
			LOGGER.error("Error in transmitDatas: {}", e.getMessage());
			JSONArray errorArray = new JSONArray();
			errorArray.put(new JSONObject().put("error", e.getMessage()));
			return errorArray;
		}
	}

	public JSONArray transmitDataspgrest(String toUrl, String schema) throws IOException {

		HttpGet httpGet = new HttpGet(URLEncode(toUrl).toString());
		httpGet.setHeader("Connection", "close");
		httpGet.setHeader("Accept-Profile", schema);

		return executeRequest(httpGet, "GET");

	}

	public String transmitDataspgrestpost(String toUrl, String data, boolean addheader, String schema)
			throws IOException {
		HttpPost httpPost = new HttpPost(toUrl);
		httpPost.addHeader("Content-Type", "application/json;charset=utf-8");
		httpPost.addHeader("Content-Profile", schema);
		if (addheader)
			httpPost.addHeader("Prefer", "return=representation");
		httpPost.setEntity(new StringEntity(data, StandardCharsets.UTF_8));

		return executeRequest(httpPost, "POST").toString();

	}

	public String transmitDataspgrestput(String toUrl, String data, boolean addheader, String schema)
			throws IOException {
		HttpPut httpPut = new HttpPut(toUrl);
		httpPut.addHeader("Content-Type", "application/json;charset=utf-8");
		httpPut.addHeader("Content-Profile", schema);
		if (addheader)
			httpPut.addHeader("Prefer", "return=representation");
		httpPut.setEntity(new StringEntity(data, ContentType.APPLICATION_JSON.withCharset(StandardCharsets.UTF_8)));

		return executeRequest(httpPut, "PUT").toString();

	}

	public String transmitDataspgrestDel(String toUrl, String schema) throws IOException {
		HttpDelete httpDel = new HttpDelete(URLEncode(toUrl).toString());
		httpDel.setHeader("Connection", "close");
		httpDel.setHeader("Content-Profile", schema);

		return executeRequest(httpDel, "DELETE").toString();

	}

	public String transmitDataPushNotification(String firebaseurl, String data) throws IOException {

		HttpPost httpPush = new HttpPost(firebaseurl);
		httpPush.addHeader("Content-Type", "application/json;charset=utf-8");
		httpPush.addHeader("Authorization", "Bearer " + getAccessToken());
		httpPush.setEntity(new StringEntity(data, StandardCharsets.UTF_8));
		return executeRequest(httpPush, "POST").toString();

	}

	public StringBuilder URLEncode(String value) {
		StringBuilder result = new StringBuilder();
		try {
//			 String regex =DisplaySingleton.memoryApplicationSetting.getString("UrlEncodeExcept");
			String regex = "[^a-zA-Z0-9=&?_.:/\\-'()!,]";
			for (int i = 0; i < value.length(); i++) {
				char c = value.charAt(i);
				if (!isArabic(c)) {
					if (String.valueOf(c).matches(regex)) {
						// URL encode the special character
						String encodedChar = URLEncoder.encode(String.valueOf(c), StandardCharsets.UTF_8.toString());
						result.append(encodedChar);
					} else {
						result.append(c);
					}
				} else {
					result.append(c);
				}
			}
		} catch (Exception e) {
			LOGGER.error("Exception at {}", Thread.currentThread().getStackTrace()[1].getMethodName(), e);
		}
		return result;
	}

	private static boolean isArabic(char c) {
		return (c >= '\u0600' && c <= '\u06FF');
	}

//	public String transmitDatasNafath(String toUrl, String data, boolean addheader) {
//		int statusCode = 0;
//		String returndata = "";
//		PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
//		connectionManager.setMaxTotal(250); // Maximum total connections
//		connectionManager.setDefaultMaxPerRoute(20);
//		try (CloseableHttpClient httpClient = HttpClients.custom().setMaxConnPerRoute(10000)
//				.setConnectionManager(connectionManager) // Max connections per //
//				.setMaxConnTotal(10000).build()) {
//
//			HttpPost http = new HttpPost(toUrl);
//			http.addHeader("Content-Type", "application/json");
//			http.addHeader("APP-ID", "35edb476");
//			http.addHeader("APP-KEY", " a102491f396c752ad80eb681223482c2");
//			if (addheader)
//				http.addHeader("Prefer", "return=representation");
//			StringEntity requestEntity = new StringEntity(data, StandardCharsets.UTF_8);
//			http.setEntity(requestEntity);
//			try (CloseableHttpResponse response = httpClient.execute(http)) {
//				statusCode = response.getStatusLine().getStatusCode();
//				String status = String.valueOf(statusCode);
//				LOGGER.error("Response Code: {}", status);
//				HttpEntity responseEntity = response.getEntity();
//				if (responseEntity != null) {
//					String responseBody = EntityUtils.toString(responseEntity);
//					LOGGER.error("Response Body: {}", responseBody);
//					if (!responseBody.equalsIgnoreCase("[]") && !responseBody.equalsIgnoreCase("")
//							&& responseBody.startsWith("{")) {
//						return returndata = new JSONObject(responseBody.toString()).toString();
//					}
//					if (!responseBody.equalsIgnoreCase("[]") && !responseBody.equalsIgnoreCase("")) {
//						if (new JSONObject(new JSONArray(responseBody).get(0).toString()).has("reflex"))
//							returndata = new JSONObject(new JSONArray(responseBody).get(0).toString())
//									.getString("reflex");
//						else
//							returndata = new JSONObject(new JSONArray(responseBody).get(0).toString()).toString();
//					} else {
//						returndata = String.valueOf(statusCode);
//					}
//				}
//				returndata = String.valueOf(statusCode);
//			}
//
//		} catch (IOException e) {
//			LOGGER.error("Excepton at :", e);
//		}
//		return returndata;
//	}

	private static List<String> SCOPES = Arrays.asList("https://www.googleapis.com/auth/cloud-platform");

	private String getAccessToken() throws IOException {
		GoogleCredentials googleCredentials;
		try (InputStream serviceAccountStream = new ByteArrayInputStream(
				ConfigurationFile.getPushNotificationAuthJson().getBytes())) {
			googleCredentials = GoogleCredentials.fromStream(serviceAccountStream).createScoped(SCOPES);
		}
		googleCredentials.refresh();
		return googleCredentials.getAccessToken().getTokenValue();

	}
	
	public String transmitDataspgrestPutbulk(String toUrl, String data, boolean addheader, String schema)
			throws IOException {
			 
			HttpPost httpPost = new HttpPost(toUrl);
			httpPost.addHeader("Content-Type", "application/json;charset=utf-8");
			httpPost.addHeader("Content-Profile", schema);
			if (addheader) {
			httpPost.addHeader("Prefer", "return=representation, resolution=merge-duplicates");
			} else {
			httpPost.addHeader("Prefer", "resolution=merge-duplicates");
			}
			 
			httpPost.setEntity(new StringEntity(data, ContentType.APPLICATION_JSON.withCharset(StandardCharsets.UTF_8)));
			 
			return executeRequest(httpPost, "POST").toString();
			 
			}

//	public String transmitDataspgrestbulkInsert(String toUrl, String data, boolean addheader, String schema) {
//		int statusCode = 0;
//		String returndata = "";
//		PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
//		connectionManager.setMaxTotal(250); // Maximum total connections
//		connectionManager.setDefaultMaxPerRoute(20);
//		try (CloseableHttpClient httpClient = HttpClients.custom().setMaxConnPerRoute(10000)
//				.setConnectionManager(connectionManager) // Max connections per //
//				.setMaxConnTotal(10000).build()) {
//
//			HttpPost http = new HttpPost(toUrl);
//			http.addHeader("Content-Type", "application/json;charset=utf-8");
//			http.addHeader("Content-Profile", schema);
//			http.addHeader("Prefer", "missing=default");
//			if (addheader)
//				http.addHeader("Prefer", "return=representation");
//			StringEntity requestEntity = new StringEntity(data, StandardCharsets.UTF_8);
//			http.setEntity(requestEntity);
//			try (CloseableHttpResponse response = httpClient.execute(http)) {
//				statusCode = response.getStatusLine().getStatusCode();
//				String status = String.valueOf(statusCode);
//				LOGGER.error("Response Code:  {}", status);
//				HttpEntity responseEntity = response.getEntity();
//				if (responseEntity != null) {
//					String responseBody = EntityUtils.toString(responseEntity);
//					LOGGER.error("Response Body: {}", responseBody);
//					if (!responseBody.equalsIgnoreCase("[]") && !responseBody.equalsIgnoreCase("")
//							&& responseBody.startsWith("{")) {
//						return returndata = new JSONObject(responseBody.toString()).toString();
//					}
//					if (!responseBody.equalsIgnoreCase("[]") && !responseBody.equalsIgnoreCase("")) {
//
//						if (new JSONObject(new JSONArray(responseBody).get(0).toString()).has("reflex"))
//							returndata = new JSONObject(new JSONArray(responseBody).get(0).toString())
//									.getString("reflex");
//						else
//							returndata = new JSONObject(new JSONArray(responseBody).get(0).toString()).toString();
//					} else {
//						returndata = String.valueOf(statusCode);
//					}
//				}
//
//			}
//
//		} catch (IOException e) {
//			LOGGER.error("Excepton at : ", e);
//		}
//		return returndata;
//	}

}
