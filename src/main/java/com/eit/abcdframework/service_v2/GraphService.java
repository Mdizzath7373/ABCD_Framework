package com.eit.abcdframework.service_v2;

import org.json.JSONArray;
import org.json.JSONObject;

public interface GraphService {
	String getChart(String data);
	
    String getDataValues(String url,String schemaName);
}
