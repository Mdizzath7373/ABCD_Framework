package com.eit.abcdframework.http.caller;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.eit.abcdframework.config.ConfigurationFile;
import com.google.auth.oauth2.GoogleCredentials;

@Component("Httpclientcaller")
public class Httpclientcaller {

	private static final Logger LOGGER = LoggerFactory.getLogger(Httpclientcaller.class);

	public JSONArray transmitDataspgrest(String toUrl) throws IOException {
		PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
		connectionManager.setMaxTotal(250); // Maximum total connections
		connectionManager.setDefaultMaxPerRoute(20);
		try (CloseableHttpClient httpClient = HttpClients.custom().setMaxConnPerRoute(10000)
				.setConnectionManager(connectionManager) // Max connections per //
				.setMaxConnTotal(10000).build()) {
			if (toUrl.contains("{"))
				toUrl = toUrl.replace("{", "%7B").replace("}", "%7D");

			if (toUrl.contains(">") || toUrl.contains("<")) {
				toUrl = toUrl.contains(">") ? toUrl.replace(">", "%3E") : toUrl;
				toUrl = toUrl.contains("<") ? toUrl.replace("<", "%3C") : toUrl;
			}

			HttpGet httpGet = new HttpGet(toUrl);
			httpGet.setHeader("Connection", "close");
			try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
				String responseBody = "";
				int statusCode = response.getStatusLine().getStatusCode();
				HttpEntity responseEntity = response.getEntity();
				if (responseEntity != null) {
					responseBody = EntityUtils.toString(responseEntity);
					if (responseBody.equalsIgnoreCase("") || responseBody.equalsIgnoreCase("{}")) {
						return new JSONArray();
					}
					if (responseBody.startsWith("{")) {
						return new JSONArray().put(new JSONObject(responseBody));
					}
				}
				String status = String.valueOf(statusCode);
				LOGGER.error("Status Code: {}", status);

				return new JSONArray(responseBody);
			}
		}
	}

	public String transmitDataspgrestpost(String toUrl, String data, boolean addheader) {
		int statusCode = 0;
		String returndata = "";
		PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
		connectionManager.setMaxTotal(250); // Maximum total connections
		connectionManager.setDefaultMaxPerRoute(20);
		try (CloseableHttpClient httpClient = HttpClients.custom().setMaxConnPerRoute(10000)
				.setConnectionManager(connectionManager) // Max connections per //
				.setMaxConnTotal(10000).build()) {

			HttpPost http = new HttpPost(toUrl);
			http.addHeader("Content-Type", "application/json;charset=utf-8");
			if (addheader)
				http.addHeader("Prefer", "return=representation");
			StringEntity requestEntity = new StringEntity(data, StandardCharsets.UTF_8);
			http.setEntity(requestEntity);
			try (CloseableHttpResponse response = httpClient.execute(http)) {
				statusCode = response.getStatusLine().getStatusCode();
				String status = String.valueOf(statusCode);
				LOGGER.error("Response Code:  {}", status);
				HttpEntity responseEntity = response.getEntity();
				if (responseEntity != null) {
					String responseBody = EntityUtils.toString(responseEntity);
					LOGGER.error("Response Body: {}", responseBody);
					if (!responseBody.equalsIgnoreCase("[]") && !responseBody.equalsIgnoreCase("")
							&& responseBody.startsWith("{")) {
						return returndata = new JSONObject(responseBody.toString()).toString();
					}
					if (!responseBody.equalsIgnoreCase("[]") && !responseBody.equalsIgnoreCase("")) {
						if (new JSONObject(new JSONArray(responseBody).get(0).toString()).has("reflex"))
							returndata = new JSONObject(new JSONArray(responseBody).get(0).toString())
									.getString("reflex");
						else
							returndata = new JSONObject(new JSONArray(responseBody).get(0).toString()).toString();
					} else {
						returndata = String.valueOf(statusCode);
					}
				}

			}

		} catch (IOException e) {
			LOGGER.error("Excepton at : ", e);
		}
		return returndata;
	}

	public String transmitDataspgrestput(String toUrl, String data, boolean addheader) {
		int statusCode = 0;
		String returndata = "";
		PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
		connectionManager.setMaxTotal(250); // Maximum total connections
		connectionManager.setDefaultMaxPerRoute(20);
		try (CloseableHttpClient httpClient = HttpClients.custom().setMaxConnPerRoute(10000)
				.setConnectionManager(connectionManager) // Max connections per //
				.setMaxConnTotal(10000).build()) {

			HttpPut http = new HttpPut(toUrl);
			http.addHeader("Content-Type", "application/json");
			if (addheader)
				http.addHeader("Prefer", "return=representation");
			StringEntity requestEntity = new StringEntity(data, StandardCharsets.UTF_8);
			http.setEntity(requestEntity);
			try (CloseableHttpResponse response = httpClient.execute(http)) {
				statusCode = response.getStatusLine().getStatusCode();
				String status = String.valueOf(statusCode);
				LOGGER.error("Response Code: {}", status);
				HttpEntity responseEntity = response.getEntity();
				if (responseEntity != null) {
					String responseBody = EntityUtils.toString(responseEntity);
					LOGGER.error("Response Body: {}", responseBody);

				}
				returndata = String.valueOf(statusCode);
			}

		} catch (IOException e) {
			LOGGER.error("Excepton at :", e);
		}
		return returndata;
	}
	public String transmitDatasNafath(String toUrl, String data, boolean addheader) {
		int statusCode = 0;
		String returndata = "";
		PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
		connectionManager.setMaxTotal(250); // Maximum total connections
		connectionManager.setDefaultMaxPerRoute(20);
		try (CloseableHttpClient httpClient = HttpClients.custom().setMaxConnPerRoute(10000)
				.setConnectionManager(connectionManager) // Max connections per //
				.setMaxConnTotal(10000).build()) {

			HttpPost http = new HttpPost(toUrl);
			http.addHeader("Content-Type", "application/json");
			http.addHeader("APP-ID","35edb476");
			http.addHeader("APP-KEY"," a102491f396c752ad80eb681223482c2");
			if (addheader)
				http.addHeader("Prefer", "return=representation");
			StringEntity requestEntity = new StringEntity(data, StandardCharsets.UTF_8);
			http.setEntity(requestEntity);
			try (CloseableHttpResponse response = httpClient.execute(http)) {
				statusCode = response.getStatusLine().getStatusCode();
				String status = String.valueOf(statusCode);
				LOGGER.error("Response Code: {}", status);
				HttpEntity responseEntity = response.getEntity();
				if (responseEntity != null) {
					String responseBody = EntityUtils.toString(responseEntity);
					LOGGER.error("Response Body: {}", responseBody);
					if (!responseBody.equalsIgnoreCase("[]") && !responseBody.equalsIgnoreCase("")
							&& responseBody.startsWith("{")) {
						return returndata = new JSONObject(responseBody.toString()).toString();
					}
					if (!responseBody.equalsIgnoreCase("[]") && !responseBody.equalsIgnoreCase("")) {
						if (new JSONObject(new JSONArray(responseBody).get(0).toString()).has("reflex"))
							returndata = new JSONObject(new JSONArray(responseBody).get(0).toString())
									.getString("reflex");
						else
							returndata = new JSONObject(new JSONArray(responseBody).get(0).toString()).toString();
					} else {
						returndata = String.valueOf(statusCode);
					}
				}
				returndata = String.valueOf(statusCode);
			}

		} catch (IOException e) {
			LOGGER.error("Excepton at :", e);
		}
		return returndata;
	}

	public int transmitDataspgrestDel(String toUrl) throws IOException {
		int statusCode = 0;
		PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
		connectionManager.setMaxTotal(250); // Maximum total connections
		connectionManager.setDefaultMaxPerRoute(20);
		try (CloseableHttpClient httpClient = HttpClients.custom().setMaxConnPerRoute(10000)
				.setConnectionManager(connectionManager) // Max connections per //
				.setMaxConnTotal(10000).build()) {

			HttpDelete httpDel = new HttpDelete(toUrl);
			try (CloseableHttpResponse response = httpClient.execute(httpDel)) {
				statusCode = response.getStatusLine().getStatusCode();
				String status = String.valueOf(statusCode);
				LOGGER.error("Response Code: {}", status);
			}

		} catch (IOException e) {
			LOGGER.error("Excepton at :", e);
		}
		return statusCode;

	}

	public JSONObject transmitDataPushNotification(String toUrl, String data) {
		int statusCode = 0;
		JSONObject returndata = null;
		PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
		connectionManager.setMaxTotal(250); // Maximum total connections
		connectionManager.setDefaultMaxPerRoute(20);
		try (CloseableHttpClient httpClient = HttpClients.custom().setMaxConnPerRoute(10000)
				.setConnectionManager(connectionManager) // Max connections per //
				.setMaxConnTotal(10000).build()) {

			HttpPost http = new HttpPost(toUrl);
			http.addHeader("Content-Type", "application/json;charset=utf-8");
			http.addHeader("Authorization", "Bearer " + getAccessToken());
			//onboard
//			http.addHeader("Authorization","key=AAAAyvLtD4s:APA91bHfGpeSqPM9CDALqbLPJH1C8_2mIShEzzTb9EOvKYxh52I4-CevU5iIj3doZ2FWp6ESpGGcanhg_0H2M6NIdkR61bMbdoEwpaXURuXDtDMb2Soy479vKnetWvp3fvQqFIBcotKj");
			StringEntity requestEntity = new StringEntity(data, StandardCharsets.UTF_8);
			http.setEntity(requestEntity);
			try (CloseableHttpResponse response = httpClient.execute(http)) {
				statusCode = response.getStatusLine().getStatusCode();
				String status = String.valueOf(statusCode);
				LOGGER.error("Response Code:  {}", status);
				HttpEntity responseEntity = response.getEntity();
				if (responseEntity != null) {
					String responseBody = EntityUtils.toString(responseEntity);
					LOGGER.warn("Response Body: {}", responseBody);
					LOGGER.warn("statusCode: {}", statusCode);
					returndata = new JSONObject(responseBody);
					System.err.println(returndata);
				}
			}

		} catch (IOException e) {
			LOGGER.error("Excepton at : ", e);
		}
		return returndata;
	}

	private static List<String> SCOPES = Arrays.asList("https://www.googleapis.com/auth/cloud-platform");

	private String getAccessToken() throws IOException {
//		ClassPathResource classpathresource = new ClassPathResource("googleAuth.json");
//		GoogleCredentials googleCredentials = GoogleCredentials.fromStream(new 0000000(writeJsonToFile(ConfigurationFile.getPushNotificationAuthJson(),)))
//				.createScoped(SCOPES);
		GoogleCredentials googleCredentials;
		try (InputStream serviceAccountStream = new ByteArrayInputStream(
				ConfigurationFile.getPushNotificationAuthJson().getBytes())) {
			googleCredentials = GoogleCredentials.fromStream(serviceAccountStream).createScoped(SCOPES);
		}
			googleCredentials.refresh();
			return googleCredentials.getAccessToken().getTokenValue();
		
	}

}
