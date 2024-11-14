package com.eit.abcdframework.serverbo;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.eit.abcdframework.controller.FormdataController;
import com.eit.abcdframework.http.caller.Httpclientcaller;
import com.eit.abcdframework.projectMaker.FileWriterComponent;
import com.eit.abcdframework.projectMaker.UtilitiesForService;

@Service
public class CodeGenerateService {
	
	
	@Value("${applicationurl}")
	private String applicationUrl;
	

	@Value("${schema}")
	private String schema;
	
//	private JSONArray tableData;
	
	Logger logger = Logger.getLogger(CodeGenerateService.class.getName());
	
	@Autowired
	FileWriterComponent fileWriter;
	
	@Autowired
	UtilitiesForService utilityService;
	
	@Autowired
	Httpclientcaller getFormData;
	
	JSONArray tableData=null;
	
	public void createController() {
		try {
		
	 tableData=new JSONArray(getFormData.transmitDataspgrest(applicationUrl+"configs",schema).toString());
		
		List<String> aliasNames=new ArrayList<String>();
		for(int i =0;i<tableData.length();i++) {
			JSONObject json = tableData.getJSONObject(i);
		
			if(json.get("displaytypes").equals("grid")) {
				aliasNames.add(json.getString("alias"));
			}
		}
		logger.info("All aliasnames with grid displaytypes : "+aliasNames.toString());
		List<Map<String,String>> list=new ArrayList<Map<String,String>>();
		String instanceForService = "\t@Autowired\n\tprivate GridService gridService;";
		for(String alias : aliasNames) {
			
			
		
		String annotation = "\t@PostMapping("+"\""+"/"+alias+"\""+")";
		
		String methodSignature = "\tpublic String "+alias+"(@RequestBody String data)"+"{";
		
		String methodBody ="\t return gridService."+alias+"(new JSONObject(data).getBoolean(\"function\"),new JSONObject(data).getString(\"role\"),new JSONObject(data).getString(\"where\"));"+"\n\t}";
		
		
		HashMap<String,String> singleMethod = new HashMap<String,String>();
		singleMethod.put("annotation", annotation);
		singleMethod.put("methodSignature", methodSignature);
		singleMethod.put("methodBody", methodBody);
		
		list.add(singleMethod);
	}
		fileWriter.writeOnGridController(instanceForService, list);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	public void createService() {
		ArrayList<Map<String,String>> allMethods = new ArrayList<Map<String,String>>();
		String datas;
		JSONArray onlyGrids = new JSONArray();//Will only contain grid displaytype
		for(int i=0;i<tableData.length();i++) {
			JSONObject json = tableData.getJSONObject(i);
			if(json.getString("displaytypes").equals("grid")) {
				onlyGrids.put(json);
			}
		}
		for(int i=0;i<onlyGrids.length();i++) {
			
			
			String discfgString = onlyGrids.getJSONObject(i).getString("discfg");//getting discfg as String from onlyGrids
			
			JSONObject discfg = new JSONObject(discfgString);//converting discfg to JSON object
			JSONObject jqxDetails = discfg.getJSONObject("jqxdetails");//Getting jqxDetails from discfg(JSON object)
			
			
			String gridWidth = jqxDetails.getString("gridwidth");
			String gridHeight = jqxDetails.getString("gridheight");
			boolean firstRowFilter = jqxDetails.getBoolean("firstrowfilter");
			JSONArray columns = jqxDetails.getJSONArray("columns");//Getting whole column array as json array
			
			//Now onwards we are getting required fields from columns in jqxdetails
			ArrayList<String> columnNames = new ArrayList<String>();
			ArrayList<String> displayNames = new ArrayList<String>();
			ArrayList<Map<String,String>> lang = new ArrayList<Map<String,String>>();
			ArrayList<Map<String,String>> columnStyle = new ArrayList<Map<String,String>>();
			for(int j=0;j<columns.length();j++) {
				JSONObject json = columns.getJSONObject(j);//this contains a single json in columns
				
				String columnName = json.getString("columnname");
				columnNames.add(columnName);
				
				String displayname = json.getString("displayfield");
				displayNames.add(displayname);
				
				Map<String,String> mapForLang = new HashMap<String,String>();
				mapForLang.put("english",json.getString("english"));
				mapForLang.put("arabic",json.getString("arabic"));
				lang.add(mapForLang);
				
				Map<String,String> mapForColumnStyle = new HashMap<String,String>();
				mapForColumnStyle.put("width","150%");
				String align =json.has("align") ? json.getString("align"):"center";
				mapForColumnStyle.put("align", align);
				mapForColumnStyle.put("cellsalign", json.getString("cellsalign"));
				columnStyle.add(mapForColumnStyle);
			}

			
			String gridWidthVariable = "\tString gridwidth = " + "\""+gridWidth + "\""+";";
			String gridHeightVariable = "\tString gridheight = " + "\""+gridHeight +"\""+";";

			String columnNamesVariable=utilityService.formStringArray(columnNames,"\tString[] columname = ");
			
			String displayNamesVariable=utilityService.formStringArray(displayNames,"\tString[] displayname = ");
			//System.out.println(displayNamesVariable);
			
			String langVariable=utilityService.formStringArrayByMapForLang2(lang, "\tString[] lang =");
			
			String columnStyleVariable =utilityService.formStringArrayByMapForLang2(columnStyle, "\tString[] columStyle =");
			
			String aliasName = onlyGrids.getJSONObject(i).getString("alias");//getting aliasname
//			String annotation = "\t@GetMapping("+"\""+"/"+aliasName+"\""+")";
			String methodSignature ="\n\tpublic String "+aliasName+"(boolean function,String role,String where){";
			
			String gridJsonVariable ="\tString gridJson = \"{\\\"datavalues\\\":\\\"[]\\\",\\\"jqdetails\\\":{}}\";";
			
			String firstRowFilterVariable = "\tboolean firstRowFilter = "+firstRowFilter+";";
			
			HashMap<String,String> fullMethod = new HashMap<String,String>();
//			fullMethod.put("annotation",annotation);
			fullMethod.put("methodSignature",methodSignature);
			fullMethod.put("gridJsonVariable", gridJsonVariable);
			fullMethod.put("langVariable",langVariable);
			fullMethod.put("columnStyleVariable", columnStyleVariable);
			fullMethod.put("gridWidthVariable",gridWidthVariable);
			fullMethod.put("gridHeightVariable", gridHeightVariable);
			fullMethod.put("columnNamesVariable",columnNamesVariable);
			fullMethod.put("displayNamesVariable",displayNamesVariable);
			fullMethod.put("firstRowFilterVariable", firstRowFilterVariable);
			fullMethod.put("showgridbyrole", (utilityService.formJSONObject(jqxDetails.get("showgridbyrole").toString(),"\tJSONObject showgridbyrole=new JSONObject(") ));
			fullMethod.put("datas", (utilityService.formJSONObject(onlyGrids.getJSONObject(i).getString("datas"),"\tJSONObject configDatas = new JSONObject(")));
			allMethods.add(fullMethod);
		}
		fileWriter.writeOnGridService(allMethods);
	}

}
