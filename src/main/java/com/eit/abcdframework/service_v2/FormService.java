package com.eit.abcdframework.service_v2;

import org.json.JSONObject;

import com.eit.abcdframework.globalhandler.GlobalAttributeHandler;

public interface FormService {
	
	String transmttingToMethod(String method,String data);
	
	String transmittingToMethodDel(String aliasName,String deleteBy,String deleteContent);
	
	String executeQuery(String data);
	
	
}
