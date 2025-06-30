package com.eit.abcdframework.service_v2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.eit.abcdframework.globalhandler.GlobalAttributeHandler;
import com.eit.abcdframework.http.caller.Httpclientcaller;
import com.eit.abcdframework.serverbo.CommonServices;
import com.eit.abcdframework.serverbo.DisplaySingleton;

@Service
public class GridServiceImplementation implements GridService{
	
	private static final Logger LOGGER = LoggerFactory.getLogger("GridServiceImplementation");
	
	@Autowired
	Httpclientcaller dataTransmit;
	
	
	@Override
	public String fetchGridJSON_v2(String data) {
		
		try {
			JSONObject finalResult = new JSONObject();
			
			JSONObject payLoad = new JSONObject(data);
			String aliasName = payLoad.getString("aliasName");
			String fetchBy = payLoad.getString("fetchBy");
			String where = payLoad.optString("where","");
			String schema = payLoad.getString("schema");
		
			
			StringBuilder urlForConfigs = new StringBuilder(GlobalAttributeHandler.getPgrestURL());
			urlForConfigs.append("configs_new").append("?").append("alias_name").append("=eq.").append(aliasName);
			LOGGER.info("URL FOR CONFIGS :"+urlForConfigs);
			JSONObject configs = dataTransmit.transmitDataspgrest(urlForConfigs.toString(),schema).getJSONObject(0);
			
			
			JSONObject configuration = new JSONObject(configs.getString("configuration"));
			LOGGER.info("configuration : "+configuration.toString());
			
			finalResult.put("display", configuration.getJSONObject("display"));
			
			
			finalResult.put("datavalues",new JSONArray(getDataValues(configuration,fetchBy,where)));
			
			return finalResult.toString();
			
		}catch(Exception e) {
			e.printStackTrace();
			return null;
		}
		
	}
	public String getDataValues(JSONObject configuration,String fetchBy,String where) {
		try {
			StringBuilder url = new StringBuilder(GlobalAttributeHandler.getPgrestURL());
			JSONObject dataSource = configuration.getJSONObject("dataSource");
			String schemaName = dataSource.getString("schema");
			if(fetchBy.equals("table")) {
				url.append(dataSource.getString("table"));
				if(!where.equalsIgnoreCase("")) {
				url.append("?").append(where);
				}
			}
			else if (fetchBy.equals("function")) {
				url.append("rpc/").append(dataSource.getString("function"));
				if(!where.equalsIgnoreCase("")) {
					url.append("?").append(where);
				}
			}
			else if (fetchBy.equals("query")) {
				url.append("rpc/").append("predefine_function").append("?");
				if(!where.equalsIgnoreCase("")) {
				String param = CommonServices.getOrderedJSONObject(new JSONObject().put("query", dataSource.getString("query")).put("where", " WHERE "+where));
				url.append("basequery=").append(param);
				}else {
					String param = CommonServices.getOrderedJSONObject(new JSONObject().put("query", dataSource.getString("query")));
					url.append("basequery=").append(param);
				}
			}
			
			LOGGER.info("URL : "+url);
			
			JSONArray result = dataTransmit.transmitDataspgrest(url.toString(),schemaName);
			
			HashMap<String,String> columnNameDataFieldMap = new HashMap<String,String>();
			JSONArray columnsArray =  configuration.getJSONObject("display").getJSONObject("config").getJSONArray("columns");
			for(int i=0;i<columnsArray.length();i++) {
				JSONObject obj = columnsArray.getJSONObject(i);
				if(obj.has("columnname"))
				columnNameDataFieldMap.put(obj.getString("datafield"), obj.getString("columnname"));
			}
			
			for (int i = 0; i < result.length(); i++) {
	            JSONObject obj = result.getJSONObject(i);
	            List<String> keysToRename = new ArrayList<>();
	            for (Iterator<String> it = obj.keys(); it.hasNext();) {
	                keysToRename.add(it.next());
	            }
	            for (String oldKey : keysToRename) {
	                if (columnNameDataFieldMap.containsKey(oldKey)) {
	                    Object value = obj.get(oldKey);
	                    String newKey = columnNameDataFieldMap.get(oldKey);
	                    obj.remove(oldKey);
	                    obj.put(newKey, value);
	                }
	            }
	        }
			return result.toString();
			
		}catch(Exception e) {
			e.printStackTrace();
			return "[]";
		}
	}
}
