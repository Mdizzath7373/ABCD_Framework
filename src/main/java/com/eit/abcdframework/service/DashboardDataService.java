package com.eit.abcdframework.service;

import org.json.JSONObject;

public interface DashboardDataService {
	
	 String handlerOfSocket(String displaytab, boolean Isfirst, String role, String Where);

	String getSocketData(JSONObject getdatas, JSONObject getConfigjson, String displaytab, JSONObject whereCondition);
	

}
