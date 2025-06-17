package com.eit.abcdframework.service_v2;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.eit.abcdframework.globalhandler.GlobalAttributeHandler;
import com.eit.abcdframework.http.caller.Httpclientcaller;
import com.eit.abcdframework.serverbo.DisplaySingleton;

@Service
public class FormServiceImplementation implements FormService{

	private static final Logger LOGGER = LoggerFactory.getLogger("FormServiceImplementation");
	
	@Autowired
	Httpclientcaller dataTransmit;
	
	@Override
	public String transmttingToMethod(String method,String data) {
		try {
			JSONObject payLoad = new JSONObject(data);
			JSONObject header = payLoad.getJSONObject("header");
			String aliasName = header.getString("aliasName");
			Boolean function = header.optBoolean("function",false);
			String response = null;
			JSONObject configuration =new JSONObject( DisplaySingleton.memoryConfigsV2.getJSONObject(aliasName).getString("configuration"));
			JSONObject dataSource = configuration.getJSONObject("dataSource");
			
			if(DisplaySingleton.memoryConfigsV2.has(aliasName)) {
			if(method.equals("POST")) {
				
					StringBuilder url = new StringBuilder(GlobalAttributeHandler.getPgrestURL());
					if(!function) {
						String tableName = dataSource.getString("table");
						url.append(tableName);
						LOGGER.info("URL FOR POST : "+url.toString());
						response = dataTransmit.transmitDataspgrestpost(url.toString(),payLoad.getJSONObject("body").toString(),false,dataSource.getString("schema"));
						
					}
					else {
						LOGGER.error("Cannot use function !!!");
						response = "Error";
					}
				
			}
			else if(method.equals("PUT")) {
				String primaryKey = dataSource.getString("primaryKey");
				if(payLoad.get("body") instanceof JSONObject) {
					
					StringBuilder url = new StringBuilder(GlobalAttributeHandler.getPgrestURL());
					String tableName = dataSource.getString("table");
					url.append(tableName);
					url.append("?").append(primaryKey).append("=").append("eq.").append(payLoad.getJSONObject("body").get(primaryKey));
					LOGGER.info("URL FOR PUT : "+url);
					response = dataTransmit.transmitDataspgrestput(url.toString(), payLoad.getJSONObject("body").toString(),
							header.has("primaryvalue") ? header.getBoolean("primaryvalue") : false,
									dataSource.getString("schema"));	
				}else {
					LOGGER.error("Wrong body format. ONLY JSONObject allowed");
					response = "Error";
				}	
			}
			}else {
				LOGGER.error("Configuration not found for "+aliasName);
				response = "Error";
			}
			return response;
		}catch(Exception e) {
			e.printStackTrace();
			return "Failed";
		}
	}

	@Override
	public String transmittingToMethodDel(String data) {
		try {
			JSONObject payLoad = new JSONObject(data);
			String aliasName = payLoad.getString("aliasName");
			String deleteBy = payLoad.getString("deleteBy");
			String deleteContent = payLoad.getString("deleteContent");
			JSONObject configuration =new JSONObject( DisplaySingleton.memoryConfigsV2.getJSONObject(aliasName).getString("configuration"));
			JSONObject dataSource = configuration.getJSONObject("dataSource");
			
			if(deleteBy.equalsIgnoreCase("primarykey")) {
				StringBuilder url = new StringBuilder(GlobalAttributeHandler.getPgrestURL());
				url.append(dataSource.getString("table"))
				   .append("?")
				   .append(dataSource.getString("primaryKey"))
				   .append("=eq.")
				   .append(deleteContent);
				LOGGER.info("URL for delete : "+url);
				String response = dataTransmit.transmitDataspgrestDel(url.toString(),dataSource.getString("schema"));
				return response;
			}
			else {
				return null;
			}
			
		}
		catch(Exception e) {
			e.printStackTrace();
			return "Failed";
		}
		
	}
	
}
