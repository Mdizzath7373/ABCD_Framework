package com.eit.abcdframework.service_v2;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.eit.abcdframework.globalhandler.GlobalAttributeHandler;
import com.eit.abcdframework.http.caller.Httpclientcaller;

@Service
public class GraphServiceImplementation implements GraphService{

	public static final Logger LOGGER = LoggerFactory.getLogger(GraphServiceImplementation.class);

	@Autowired
	Httpclientcaller dataTransmit;
	
	@Override
	public String getChart(String data) {
		
		try {
			
			JSONObject payLoad = new JSONObject(data);
			String aliasName = payLoad.getString("aliasName");
			String where = payLoad.optString("where","");
			String schemaName = payLoad.getString("schema");
			
			JSONObject finalResult = new JSONObject();
			
			StringBuilder urlForConfigs = new StringBuilder(GlobalAttributeHandler.getPgrestURL());
			urlForConfigs.append("configs_new").append("?").append("alias_name").append("=eq.").append(aliasName);
			JSONObject configs = dataTransmit.transmitDataspgrest(urlForConfigs.toString(),schemaName).getJSONObject(0);
			
			
			
			JSONObject configuration = new JSONObject(configs.getString("configuration"));
			
			
			finalResult.put("display", configuration.getJSONObject("display"));
			
			
			JSONObject dataSource = configuration.getJSONObject("dataSource");
			
			JSONObject objForgetChart = new JSONObject();
			objForgetChart.put("query",dataSource.getString("query") );
			
			StringBuilder url = new StringBuilder(GlobalAttributeHandler.getPgrestURL());

			
			url.append("rpc/get_chart").append("?json_input=").append(objForgetChart.toString());
			
			LOGGER.info("URL : "+url.toString());
			
			JSONArray res = dataTransmit.transmitDataspgrest(url.toString(),schemaName );
			
			JSONObject dataValues = new JSONObject();
			
			for(int i =0;i<res.length();i++) {
				
				JSONObject obj = res.getJSONObject(i);
				
				if(dataValues.has("x")) {
					dataValues.getJSONArray("x").put(obj.getString("x"));
				}
				else {
					dataValues.put("x",new JSONArray().put(obj.getString("x")));
				}
				
				if(dataValues.has("y")) {
					dataValues.getJSONArray("y").put(obj.getString("y"));
				}
				else {
					dataValues.put("y",new JSONArray().put(obj.getString("y")));
				}
			}
			
			finalResult.put("datavalues", dataValues);
			
			return finalResult.toString();
		}catch(Exception e) {
			e.printStackTrace();
			return "Failed";
			
		}
	}

}
