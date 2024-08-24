package com.eit.abcdframework.service;

import org.json.JSONArray;
import org.json.JSONObject;

public interface BaseLoginService {

	String authenticateUser(JSONArray user, String token);

	String verification(String name,String email,String lang);

	String pushNotificationUpdate(boolean token, JSONObject user, String id, String notifitoke,JSONObject notifiJson);
	

}
