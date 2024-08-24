package com.eit.abcdframework.http.caller;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class Httpurlconnectioncaller {
	private static final Logger LOGGER = LoggerFactory.getLogger(Httpurlconnectioncaller.class);

	private static final String CONTENT_TYPE = "Content-Type";
	private static final String APPLICATION_JSON = "application/json";
	private static final String ERROR = "Error";



	public static int transmitDatas(String authorization, String method, String toUrl, String data) throws IOException {
		String response = "";
		int responseCode = 0;

		if (toUrl != null && !toUrl.equalsIgnoreCase(ERROR)) {
			HttpURLConnection conn = null;
			OutputStream os = null;
			try {
				URL url = new URL(toUrl);
				conn = (HttpURLConnection) url.openConnection();
				conn.setRequestMethod(method);
				conn.setConnectTimeout(10000);
				conn.setRequestProperty("Authorization", authorization);
				conn.setRequestProperty(CONTENT_TYPE, APPLICATION_JSON);
				conn.setDoOutput(true);
				conn.setDoInput(true);
				os = conn.getOutputStream();
				os.write(data.getBytes(StandardCharsets.UTF_8));
				responseCode = conn.getResponseCode();
				response = conn.getResponseMessage();
				LOGGER.info("DataMigration rawData = {}  Responce = {} Res = {}", data, responseCode, response);

			} catch (Exception e) {
				LOGGER.error("DataMigration rawData = {}  Responce = Error {}", data, e);

			} finally {
				if (os != null) {
					os.close();
				}
				if (conn != null) {
					conn.disconnect();
				}
			}
		}

		return responseCode;
	}

	public static JSONObject transmitDatas(String authorization, String method, String toUrl) {
		JSONObject json = null;
		int responseCode = 0;

		if (toUrl != null && !toUrl.equalsIgnoreCase(ERROR)) {
			HttpURLConnection conn = null;
			try {
				URL url = new URL(toUrl);
				conn = (HttpURLConnection) url.openConnection();
				conn.setRequestMethod(method);
				conn.setConnectTimeout(10000);
				conn.setRequestProperty("Authorization", authorization);
				conn.setRequestProperty(CONTENT_TYPE, APPLICATION_JSON);
				responseCode = conn.getResponseCode();
				if (responseCode == HttpURLConnection.HTTP_OK) { // success
					BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
					String inputLine;

					while ((inputLine = in.readLine()) != null) {
						json = new JSONObject(inputLine);
					}

					in.close();

				}
			} catch (Exception e) {
				LOGGER.error("Exception at  ", e);
			}

		}

		return json;
	}

	public static int transmitDataspgrest(String method, String toUrl, String data) throws IOException {
		String response = "";
		int responseCode = 0;

		if (toUrl != null && !toUrl.equalsIgnoreCase(ERROR)) {
			HttpURLConnection conn = null;
			OutputStream os = null;
			try {
				URL url = new URL(toUrl);
				conn = (HttpURLConnection) url.openConnection();
				conn.setRequestMethod(method);
				conn.setDoOutput(true);
				conn.setDoInput(true);
				conn.setConnectTimeout(10000);
				conn.setRequestProperty("Connection", "close");
				conn.setRequestProperty(CONTENT_TYPE, APPLICATION_JSON);
				os = conn.getOutputStream();
				os.write(data.getBytes(StandardCharsets.UTF_8));
				responseCode = conn.getResponseCode();
				response = conn.getResponseMessage();
				LOGGER.info("DataMigration rawData = {}  Responce = {} Res = {}", data, responseCode, response);

			} catch (Exception e) {
				LOGGER.error("DataMigration rawData = {}  Error", data, e);

			} finally {
				if (os != null) {
					os.close();
				}
				if (conn != null) {
					conn.disconnect();
				}
			}
		}

		return responseCode;
	}

	public static JSONArray transmitDataspgrest(String method, String toUrl) {
		StringBuilder textBuilder = new StringBuilder();
		JSONArray jsonArray = new JSONArray();
		int responseCode = 0;

		if (toUrl != null && !toUrl.equalsIgnoreCase(ERROR)) {
			HttpURLConnection conn = null;
			try {
				URL url = new URL(toUrl);
				conn = (HttpURLConnection) url.openConnection();
				conn.setRequestMethod(method);
				conn.setRequestProperty("Connection", "close");
				responseCode = conn.getResponseCode();
				if (responseCode == HttpURLConnection.HTTP_OK) { // success
					InputStream inputStream = conn.getInputStream();
					textBuilder = new StringBuilder();
					try (Reader reader = new BufferedReader(
							new InputStreamReader(inputStream, Charset.forName(StandardCharsets.UTF_8.name())))) {
						int c = 0;
						while ((c = reader.read()) != -1) {
							textBuilder.append((char) c);
						}
					}
					jsonArray = new JSONArray(textBuilder.toString());

					conn.disconnect();

				}
			} catch (Exception e) {
				LOGGER.error("Exception at ", e);
			}

		}

		return jsonArray;
	}
}
