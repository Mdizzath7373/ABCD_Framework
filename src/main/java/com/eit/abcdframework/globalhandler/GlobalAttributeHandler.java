package com.eit.abcdframework.globalhandler;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.eit.abcdframework.serverbo.DisplaySingleton;

import jakarta.annotation.PostConstruct;

@ControllerAdvice
public class GlobalAttributeHandler {

	private static final String KEY = "primarykey";
	private static final String primaryColumnKey = "columnname";
	private static final String REFLEX = "reflex";
	private static final String SUCCESS = "Success";
	private static final String ERROR = "error";
	private static final String FAILURE = "Failure";
	private static final String DATAVALUE = "datavalue";


	private static String schemas;
	private static String pgrestURL;

	private static JSONObject getPushNotificationJsonObject = DisplaySingleton.memoryApplicationSetting
			.has("notificationConfig")
					? new JSONObject(DisplaySingleton.memoryApplicationSetting.get("notificationConfig").toString())
							.getJSONObject("sendnotification")
					: new JSONObject();

	@Value("${applicationurl}")
	private String pgrest;

	@Value("${schema}")
	private String schema;

	@PostConstruct
	public void init() {
		schemas = schema;
		pgrestURL = pgrest;

	}

	@ModelAttribute("notificationConfig")
	public static JSONObject getNotificationConfig() {
		return getPushNotificationJsonObject;
	}
	
	@ModelAttribute("schemas")
	public static String getSchemas() {
		return schemas;
	}

	@ModelAttribute("pgrestURL")
	public static String getPgrestURL() {
		return pgrestURL;
	}


	@ModelAttribute("KEY")
	public static String getKey() {
		return KEY;
	}

	@ModelAttribute("primaryColumnKey")
	public static String getPrimarycolumnkey() {
		return primaryColumnKey;
	}

	@ModelAttribute("REFLEX")
	public static String getReflex() {
		return REFLEX;
	}

	@ModelAttribute("SUCCESS")
	public static String getSuccess() {
		return SUCCESS;
	}

	@ModelAttribute("ERROR")
	public static String getError() {
		return ERROR;
	}

	@ModelAttribute("FAILURE")
	public static String getFailure() {
		return FAILURE;
	}

	@ModelAttribute("DATAVALUE")
	public static String getDatavalue() {
		return DATAVALUE;
	}

}
