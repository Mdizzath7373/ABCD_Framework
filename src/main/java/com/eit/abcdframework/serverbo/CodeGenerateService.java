package com.eit.abcdframework.serverbo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Collectors;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.eit.abcdframework.http.caller.Httpclientcaller;
import com.eit.abcdframework.projectMaker.FileWriterComponent;
import com.eit.abcdframework.projectMaker.UtilitiesForService;

@Service
public class CodeGenerateService {

	@Value("${applicationurl}")
	private String applicationUrl;

//	private JSONArray tableData;

	Logger logger = Logger.getLogger(CodeGenerateService.class.getName());

	@Autowired
	FileWriterComponent fileWriter;

	@Autowired
	UtilitiesForService utilityService;

	@Autowired
	Httpclientcaller getFormData;

	JSONArray tableData = null;

	public void start(String schecmaName) {
		try {
			tableData = new JSONArray(getFormData
					.transmitDataspgrest(applicationUrl + "configs?displaytypes=eq.form", schecmaName).toString());
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		HashSet<String> displayTypesInTable = new HashSet<String>();
		for (int i = 0; i < tableData.length(); i++) {
			displayTypesInTable.add(tableData.getJSONObject(i).getString("displaytypes"));
		}

		System.out.println(displayTypesInTable.toString());
		for (String displayType : displayTypesInTable) {
			create(displayType);
		}
	}

	@Async
	public void create(String displayType) {
		if (displayType.equals("grid") || displayType.equals("widgets"))
			createController(displayType);
		else if (displayType.equals("form"))
			createControllerForForm();

		if (displayType.equals("grid")) {
			createServiceForGrid();
		} else if (displayType.equals("widgets")) {
			createServiceForWidget();
		} else if (displayType.equals("form")) {
			createServiceForForms();
		}

	}

	public void createController(String displayType) {
		String gridControllerFilePath = "F:\\New Git Clones\\demo\\src\\main\\java\\com\\example\\project\\controller\\GridController.java";
		String widgetControllerFilePath = "F:\\New Git Clones\\demo\\src\\main\\java\\com\\example\\project\\controller\\WidgetController.java";
		String controllerPath = (displayType.equals("grid")) ? gridControllerFilePath
				: (displayType.equals("widgets")) ? widgetControllerFilePath : "";

		List<String> aliasNames = new ArrayList<String>();
		for (int i = 0; i < tableData.length(); i++) {
			JSONObject json = tableData.getJSONObject(i);
			if (json.get("displaytypes").equals(displayType)) {
				aliasNames.add(json.getString("alias"));
			}
		}

		ArrayList<ArrayList<String>> list = new ArrayList<ArrayList<String>>();

		String instanceForService = displayType.equals("grid") ? "\t@Autowired\n\tprivate GridService Service;"
				: displayType.equals("widgets") ? "\t@Autowired\n\tprivate WidgetService Service;" : "";
		for (String alias : aliasNames) {

			String annotation = "\t@PostMapping(" + "\"" + "/" + alias.replaceAll("/", "_") + "\"" + ")";

			String methodSignature = "\tpublic String " + alias.replaceAll("/", "_") + "(@RequestBody String data)"
					+ "{";

			String methodBody = "\t JSONObject json = new JSONObject(data);\n" + "\t return Service."
					+ alias.replaceAll("/", "_")
					+ "(json.getString(\"where\"),json.has(\"role\")?json.getString(\"role\"):\"default\");" + "\n\t}";

			// System.out.println(instanceForService+"\n"+annotation+"\n"+methodSignature+"\n"+methodBody);

			ArrayList<String> singleMethod = new ArrayList<String>();
			singleMethod.add(annotation);
			singleMethod.add(methodSignature);
			singleMethod.add(methodBody);

			list.add(singleMethod);
		}
		// System.out.println(list.toString());
		fileWriter.writeController(instanceForService, list, controllerPath);
	}

	public void createServiceForGrid() {
		ArrayList<ArrayList<String>> allMethods = new ArrayList<ArrayList<String>>();
		Map<String, List<String>> pageConfig = new HashMap<>();
		String datas;
		JSONArray onlyGrids = new JSONArray();// Will only contain grid displaytype
		for (int i = 0; i < tableData.length(); i++) {
			JSONObject json = tableData.getJSONObject(i);
			if (json.getString("displaytypes").equals("grid")) {
				onlyGrids.put(json);
			}
		}
		for (int i = 0; i < onlyGrids.length(); i++) {
			String discfgString = onlyGrids.getJSONObject(i).getString("discfg");// getting discfg as String from
																					// onlyGrids

			JSONObject discfg = new JSONObject(discfgString);// converting discfg to JSON object
			JSONObject jqxDetails = discfg.getJSONObject("jqxdetails");// Getting jqxDetails from discfg(JSON object)

			String gridWidth = jqxDetails.getString("gridwidth");
			String gridHeight = jqxDetails.getString("gridheight");
			boolean firstRowFilter = jqxDetails.getBoolean("firstrowfilter");
			JSONArray columns = jqxDetails.getJSONArray("columns");// Getting whole column array as json array

			// Now onwards we are getting required fields from columns in jqxdetails
			ArrayList<String> columnNames = new ArrayList<String>();
			ArrayList<String> displayNames = new ArrayList<String>();
			ArrayList<Map<String, String>> lang = new ArrayList<Map<String, String>>();
			ArrayList<Map<String, String>> columnStyle = new ArrayList<Map<String, String>>();
			for (int j = 0; j < columns.length(); j++) {
				JSONObject json = columns.getJSONObject(j);// this contains a single json in columns

				String columnName = json.getString("columnname");
				columnNames.add(columnName);

				String displayname = json.getString("displayfield");
				displayNames.add(displayname);

				Map<String, String> mapForLang = new HashMap<String, String>();
				mapForLang.put("english", json.getString("english"));
				mapForLang.put("arabic", json.getString("arabic"));
				lang.add(mapForLang);

				Map<String, String> mapForColumnStyle = new HashMap<String, String>();
				mapForColumnStyle.put("width", "150");
				String align = json.has("align") ? json.getString("align") : "center";
				mapForColumnStyle.put("align", align);
				mapForColumnStyle.put("cellsalign", json.getString("cellsalign"));
				columnStyle.add(mapForColumnStyle);
			}

			String gridWidthVariable = "\tString gridwidth = " + "\"" + gridWidth + "\"" + ";";
			String gridHeightVariable = "\tString gridheight = " + "\"" + gridHeight + "\"" + ";";

			String columnNamesVariable = utilityService.formStringArray(columnNames, "\tString[] columname = ");

			String displayNamesVariable = utilityService.formStringArray(displayNames, "\tString[] displayname = ");
			// System.out.println(displayNamesVariable);

			String langVariable = utilityService.formStringArrayByMapForLang2(lang, "\tString[] lang =");

			String columnStyleVariable = utilityService.formStringArrayByMapForLang2(columnStyle,
					"\tString[] columStyle =");

			String aliasName = onlyGrids.getJSONObject(i).getString("alias");// getting aliasname

			String methodSignature = "\n\tpublic String " + aliasName.replaceAll("/", "_")
					+ "(String where,String role){";

			String gridJsonVariable = "\tString gridJson = \"{\\\"datavalues\\\":\\\"[]\\\",\\\"jqdetails\\\":{}}\";";

			String firstRowFilterVariable = "\tboolean firstRowFilter = " + firstRowFilter + ";";

			String urlVariable = createURLVariableForGrid(onlyGrids.getJSONObject(i).getString("datas"));

			ArrayList<String> fullMethod = new ArrayList<String>();
			fullMethod.add(methodSignature);
			fullMethod.add(gridJsonVariable);
			fullMethod.add(langVariable);
			fullMethod.add(columnStyleVariable);
			fullMethod.add(gridWidthVariable);
			fullMethod.add(gridHeightVariable);
			fullMethod.add(columnNamesVariable);
			fullMethod.add(displayNamesVariable);
			fullMethod.add(firstRowFilterVariable);
			fullMethod.add((utilityService.formJSONObject(jqxDetails.get("showgridbyrole").toString(),
					"\tJSONObject showgridbyrole=new JSONObject(")));
			fullMethod.add(urlVariable);
			fullMethod.add(utilityService
					.formJSONArray(jqxDetails.has("default") ? jqxDetails.getJSONArray("default").toString()
							: new JSONArray().toString(), "\tJSONArray action = "));
//			if (jqxDetails.has("hyperlink"))
			fullMethod.add(
					jqxDetails.has("hyperlink") ? "\tString hyperlink = \"" + jqxDetails.getString("hyperlink") + "\";"
							: "\tString hyperlink = \"\";");
//			fullMethod.add((utilityService.formJSONObject(onlyGrids.getJSONObject(i).getString("datas"),"\tJSONObject configDatas = new JSONObject(")));
			allMethods.add(fullMethod);

//			if(pageConfig.containsKey(onlyGrids.getJSONObject(i).getString("menu"))){
//              List<String> tab=	pageConfig.get(onlyGrids.getJSONObject(i).getString("menu"));
//              tab.add(onlyGrids.getJSONObject(i).getString("tab"));
//              pageConfig.put(onlyGrids.getJSONObject(i).getString("menu")+"-"+onlyGrids.getJSONObject(i).getString("alias"), tab);
//			}else {
//				List<String> tab=new ArrayList<>();
//				tab.add(onlyGrids.getJSONObject(i).getString("tab"));
//				pageConfig.put(onlyGrids.getJSONObject(i).getString("menu")+"-"+onlyGrids.getJSONObject(i).getString("alias"),tab);
//			}

		}
		fileWriter.writeOnGridService(allMethods);
// 		fileWriter.pageHtml(pageConfig);
	}

	public void createHTML() {

	}

	private String createURLVariableForGrid(String configDatas) {
		JSONObject configDatasJson = new JSONObject(configDatas);
		StringBuilder finalResult = new StringBuilder();
		finalResult.append("\n\tString url = pgresturl;\n").append("\tString api = ")
				.append(configDatasJson.has("Function") ? "\"" + configDatasJson.getString("Function") + "\";\n"
						: "\"" + configDatasJson.getString("api") + "\";\n");

		finalResult.append("\turl+=(!where.isEmpty())? api+where : api;\n");
		return finalResult.toString();
	}

	public void createServiceForWidget() {
		ArrayList<ArrayList<String>> allMethods = new ArrayList<ArrayList<String>>();

		for (int i = 0; i < tableData.length(); i++) {
			if (tableData.getJSONObject(i).getString("displaytypes").equals("widgets")) {
				JSONObject json = tableData.getJSONObject(i);

				String datasFromTable = json.getString("datas").trim();

				String discfg = json.getString("discfg");
				JSONObject jqxDetails = new JSONObject(discfg).getJSONObject("jqxdetails");
				System.out.println("jqz details : " + jqxDetails);

				JSONArray singleCardWidget = jqxDetails.getJSONArray(new JSONObject(datasFromTable).getString("name"));
				ArrayList<String> widgetNames = new ArrayList<String>();
				ArrayList<String> displayFields = new ArrayList<String>();
				String showWidgetsByRole = jqxDetails.getJSONObject("showwidgetsbyrole").toString();

				ArrayList<Map<String, String>> columnStyle = new ArrayList<Map<String, String>>();
				for (int j = 0; j < singleCardWidget.length(); j++) {
					JSONObject eachJson = singleCardWidget.getJSONObject(j);
					widgetNames.add(eachJson.getString("widgetname"));

					displayFields.add(eachJson.getString("displayfield"));

					HashMap<String, String> mapForColumnStyle = new HashMap<String, String>();
					mapForColumnStyle.put("align", eachJson.getString("align"));
					mapForColumnStyle.put("size", eachJson.has("size") ? eachJson.getString("size") : "");
					mapForColumnStyle.put("icon", eachJson.getString("icon"));
					columnStyle.add(mapForColumnStyle);

				}
				System.out.println("Datas from table : " + datasFromTable);
//				String widgetWidthVariable = "\tString gridWidth = "+"\""+jqxDetails.getString("gridwidth")+"\""+";";
//				String widgetHeightVariable = "\tString gridHeight = "+"\""+jqxDetails.getString("gridheight")+"\""+";";
//				String datasVariable = utilityService.formJSONObject(datasFromTable,
//				 		"\tJSONObject configDatas = new JSONObject(");
				String columnStyleVariable = utilityService.formStringArrayByMapForLang2(columnStyle,
						"\tString[] columnStyle =");
				String widgetNameVariable = utilityService.formStringArray(widgetNames, "\tString[] widgetName =");
				String displayFieldsVariable = utilityService.formStringArray(displayFields,
						"\tString[] displayName =");
				String showWidgetsByRoleVariable = utilityService.formJSONObject(showWidgetsByRole,
						"\tJSONObject showwidgetsbyrole = new JSONObject(");

				String aliasName = json.getString("alias");

				String methodSignature = "\tpublic String " + aliasName.replaceAll("/", "_")
						+ "(String where,String role){";
				String widgetJsonVariable = "\tString widgetJson = \"{\\\"datavalue\\\":\\\"[]\\\",\\\"jqdetails\\\":{}}\";";
				String urlVariable = createURLVariableForWidget(datasFromTable);
				String widgetsname = "\tString widgetname=\"" + new JSONObject(datasFromTable).getString("name")
						+ "\";";
				ArrayList<String> fullMethod = new ArrayList<String>();
				fullMethod.add(methodSignature);
				fullMethod.add(widgetJsonVariable);
				fullMethod.add(widgetsname);
//				fullMethod.add(widgetHeightVariable);
//				fullMethod.add(datasVariable);
				fullMethod.add(columnStyleVariable);
				fullMethod.add(widgetNameVariable);
				fullMethod.add(displayFieldsVariable);
				fullMethod.add(showWidgetsByRoleVariable);
				fullMethod.add(urlVariable);
				allMethods.add(fullMethod);
			}
		}
		fileWriter.writeOnWidgetService(allMethods);
	}

	public String createURLVariableForWidget(String configDatas) {
		JSONObject configDatasJson = new JSONObject(configDatas);
		StringBuilder finalResult = new StringBuilder();
		finalResult.append("\n\tString url = pgresturl;\n").append("\tString api = ")
				.append(configDatasJson.has("Function") ? "\"" + configDatasJson.getString("Function") + "\";\n"
						: "\"" + configDatasJson.getString("api") + "\";\n");
		// String finalUrl="";
		finalResult.append("\turl += api;\n");
		String params = "";
		if (configDatasJson.has("params")) {

			for (int i = 0; i < configDatasJson.getJSONArray("params").length(); i++) {
				params += configDatasJson.getJSONArray("params").get(i).toString() + " = "
						+ configDatasJson.getJSONArray("values").get(i).toString();
			}
		}

		finalResult.append("\n\tif(where != \"\"){");
		finalResult.append("\n\turl+=")
				.append(configDatasJson.has("params") ? "where + " + "\"&" + params + "\"" + ";\n\t}" : "where;\n\t}");
		finalResult.append("\n\telse{").append("\n\turl+=\"?\"" + params + ";\n\t}");
		return finalResult.toString();

	}

	public void createControllerForForm() {
		ArrayList<ArrayList<String>> allControllerMethods = new ArrayList<ArrayList<String>>();
		for (int i = 0; i < tableData.length(); i++) {
			JSONObject singleRow = tableData.getJSONObject(i);
			if (singleRow.getString("displaytypes").equalsIgnoreCase("form")) {
				JSONObject datas = new JSONObject(singleRow.getString("datas"));
				String alias = singleRow.getString("alias");
				if (datas.has("POST")) {
					ArrayList<String> singleFunction = new ArrayList<String>();
					singleFunction.add("\t@PostMapping(\"/" + alias.replaceAll("/", "_") + "_post\")");
					singleFunction.add("\tpublic String " + alias.replaceAll("/", "_")
							+ "_post(@RequestBody String body,@RequestParam String msg,@RequestParam String lang,@RequestParam boolean notification){");
					singleFunction.add("\t return service." + alias.replaceAll("/", "_")
							+ "_post(body,msg,lang,notification);\n\t}");
					allControllerMethods.add(singleFunction);
				}
				if (datas.has("PUT")) {
					ArrayList<String> singleFunction = new ArrayList<String>();
					singleFunction.add("\t@PutMapping(\"/" + alias.replaceAll("/", "_") + "_put\")");
					singleFunction.add("\tpublic String " + alias.replaceAll("/", "_")
							+ "_put(@RequestBody String body,@RequestParam String msg,@RequestParam String lang,@RequestParam boolean notification){");
					singleFunction.add("\t return service." + alias.replaceAll("/", "_")
							+ "_put(body,msg,lang,notification);\n\t}");
					allControllerMethods.add(singleFunction);
				}
				if (datas.has("GET")) {
					ArrayList<String> singleFunction = new ArrayList<String>();
					singleFunction.add("\t@GetMapping(\"/" + alias.replaceAll("/", "_") + "_get\")");
					singleFunction
							.add("\tpublic String " + alias.replaceAll("/", "_") + "_get(@RequestParam String where){");
					singleFunction.add("\t return service." + alias.replaceAll("/", "_") + "_get(where);\n\t}");
					allControllerMethods.add(singleFunction);
				}
				if (datas.has("DELETE")) {
					ArrayList<String> singleFunction = new ArrayList<String>();
					singleFunction.add("\t@DeleteMapping(\"/" + alias.replaceAll("/", "_") + "_delete\")");
					singleFunction.add("\tpublic String " + alias.replaceAll("/", "_")
							+ "_delete(@RequestParam String priamryId,@RequestParam String where,@RequestParam boolean isdeleteall){");
					singleFunction.add("\t return service." + alias.replaceAll("/", "_")
							+ "_delete(priamryId,where,isdeleteall);\n\t}");
					allControllerMethods.add(singleFunction);
				}
			}
		}

		String instanceForService = "\t@Autowired\n\tFormService service;";
		String formControllerFilePath = "F:\\New Git Clones\\demo\\src\\main\\java\\com\\example\\project\\controller\\FormController.java";
		fileWriter.writeController(instanceForService, allControllerMethods, formControllerFilePath);
	}

	public void createServiceForForms() {
		ArrayList<ArrayList<String>> allFunctions = new ArrayList<ArrayList<String>>();
		ArrayList<ArrayList<String>> anotherLogic = new ArrayList<ArrayList<String>>();
	
		for (int i = 0; i < tableData.length(); i++) {
			JSONObject singleRow = tableData.getJSONObject(i);

			if (singleRow.getString("displaytypes").equalsIgnoreCase("form")) {
				String aliasName = singleRow.getString("alias");
				JSONObject datas = new JSONObject(singleRow.getString("datas"));

				if (datas.has("POST")) {
					String methodSignature = "\tpublic String " + aliasName.replaceAll("/", "_")
							+ "_post(String body,String msg,String lang,boolean notification){";
					String postFromDataVariable = "\tString postFromdata = " + "\"" + datas.getString("POST") + "\""
							+ ";";
//				String schemaVariable ="\tString schema = "+"\""+datas.getString("schema")+"\""+";";
//					String primaryValueVariable = "\tboolean primaryValue = false;";
					ArrayList<String> singleFunction = new ArrayList<String>();
					singleFunction.add(methodSignature);
					singleFunction.add(postFromDataVariable);
//				singleFunction.add(schemaVariable );
//					singleFunction.add(primaryValueVariable);
					ArrayList<String> servicecll=new ArrayList<>();
					if (datas.has("sms"))
						servicecll.add("\tmsgService.smsService(body,msg);");
					if (datas.has("activityLogs")) {
						String activityObj = utilityService.formJSONObject(
								datas.getJSONObject("activityLogs").getJSONObject("setvalue").toString(),
								"\tJSONObject activityObj=new JSONObject(");
						String columnNameofdata = datas.getJSONObject("activityLogs").getJSONObject("getvalues")
								.getString("data");
						String columnNameofids = datas.getJSONObject("activityLogs").getJSONObject("getvalues")
								.getString("ids");
						servicecll.add("\tString id=new JSONObject(body).get(\" "+columnNameofids+"\").toString();");
						servicecll.add("\tString datas=new JSONObject(body).get(\" "+columnNameofdata+"\").toString();");
						servicecll.add(activityObj);
						servicecll
								.add("\tcommonServices.addactivitylog(id,datas,\"add\",activityObj,\"\",msg,notification);");
					}
					if (datas.has("email")) {
						JSONObject emailObj = datas.getJSONObject("email");
						servicecll.add(
								"\tString contentName=" + "\"" + emailObj.getString("getContentNameColumn") + "\";");
						if (emailObj.getBoolean("mailid"))
							servicecll.add("\tString mailidColumn=" + "\""
									+ emailObj.getJSONObject("getcolumn").getString("columnname") + "\";");

						JSONObject email = new JSONObject();
						emailObj.getJSONArray("getContantType").toList().stream().forEach(entry -> {
							JSONObject json = new JSONObject();

							json.put("content", emailObj.getJSONObject("mail").getJSONArray(entry.toString()).get(0));

							json.put("replaceText",
									emailObj.getJSONObject(
											emailObj.getJSONObject("mail").getJSONArray(entry.toString()).get(0)
													+ "replacementContent")
											.getJSONArray("replace"));

							json.put("replacecolumn",
									emailObj.getJSONObject(
											emailObj.getJSONObject("mail").getJSONArray(entry.toString()).get(0)
													+ "replacementContent")
											.getJSONArray("column"));
							email.put(entry.toString(), json);
						});
						servicecll.add(utilityService.formJSONObject(email.toString(),
								"\tJSONObject emailObj=new JSONObject("));
						servicecll.add(
								"\tmsgService.emailService(emailObj, new JSONObject(body), mailidColumn, contentName, lang.equalsIgnoreCase(\"\") ? \"en\" : lang);");
					}
					allFunctions.add(singleFunction);
					anotherLogic.add(servicecll);
				}
				if (datas.has("PUT")) {
					String methodSignature = "\tpublic String " + aliasName.replaceAll("/", "_")
							+ "_put(String body,String msg,String lang,boolean notification){";
					String primaryKeyVariable = "\tString columnName=\""
							+ datas.getJSONObject("primarykey").getString("columnname") + "\";";
					String putFromDataVariable = "\tString putFromdata = " + "\"" + datas.getString("PUT") + "\"" + ";";
//					String schemaVariable ="\tString schema = "+"\""+datas.getString("schema")+"\""+";";
//					String primaryValueVariable = "\tboolean primaryValue = false;";
					ArrayList<String> singleFunction = new ArrayList<String>();
					singleFunction.add(methodSignature);
					singleFunction.add(putFromDataVariable);
//					singleFunction.add(schemaVariable );
					singleFunction.add(primaryKeyVariable);
//					singleFunction.add(primaryValueVariable);
					ArrayList<String> servicecll=new ArrayList<>();
					if (datas.has("sms"))
						servicecll.add("\tmsgService.smsService(body,msg);");
					if (datas.has("activityLogs")) {
						String activityObj = utilityService.formJSONObject(
								datas.getJSONObject("activityLogs").getJSONObject("setvalue").toString(),
								"\tJSONObject activityObj=new JSONObject(");
						String columnNameofdata = datas.getJSONObject("activityLogs").getJSONObject("getvalues")
								.getString("data");
						String columnNameofids = datas.getJSONObject("activityLogs").getJSONObject("getvalues")
								.getString("ids");
						servicecll.add("\tString id=new JSONObject(body).get(\" "+columnNameofids+"\");");
						servicecll.add("\tString datas=new JSONObject(body).get(\" "+columnNameofdata+"\");");
						servicecll.add(activityObj);
						servicecll
								.add("\tcommonServices.addactivitylog(id,datas,\"edit\",activityObj,\"\",msg,notification);");
					}
					if (datas.has("email")) {
						JSONObject emailObj = datas.getJSONObject("email");
						servicecll.add(
								"\tString contentName=" + "\"" + emailObj.getString("getContentNameColumn") + "\";");
						if (emailObj.getBoolean("mailid"))
							servicecll.add("\tString mailidColumn=" + "\""
									+ emailObj.getJSONObject("getcolumn").getString("columnname") + "\";");

						JSONObject email = new JSONObject();
						emailObj.getJSONArray("getContantType").toList().stream().forEach(entry -> {
							JSONObject json = new JSONObject();

							json.put("content", emailObj.getJSONObject("mail").getJSONArray(entry.toString()).get(0));

							json.put("replaceText",
									emailObj.getJSONObject(
											emailObj.getJSONObject("mail").getJSONArray(entry.toString()).get(0)
													+ "replacementContent")
											.getJSONArray("replace"));

							json.put("replacecolumn",
									emailObj.getJSONObject(
											emailObj.getJSONObject("mail").getJSONArray(entry.toString()).get(0)
													+ "replacementContent")
											.getJSONArray("column"));
							email.put(entry.toString(), json);
						});
						servicecll.add(utilityService.formJSONObject(email.toString(),
								"\tJSONObject emailObj=new JSONObject("));
						servicecll.add(
								"\tmsgService.emailService(emailObj, new JSONObject(body), mailidColumn, contentName, lang.equalsIgnoreCase(\"\") ? \"en\" : lang);");
					}
					allFunctions.add(singleFunction);
					anotherLogic.add(servicecll);
				}
				if (datas.has("GET")) {
					String methodSignature = "\tpublic String " + aliasName.replaceAll("/", "_")
							+ "_get(String where){";
					String getFromDataVariable = "\tString getFromdata = " + "\"" + datas.getString("GET") + "\"" + ";";
//					String methodVariable = "\tString columnName=\""
//							+ datas.getJSONObject("primarykey").getString("columnname") + "\";";
//					String schemaVariable ="\tString schema = "+"\""+datas.getString("schema")+"\""+";";
					ArrayList<String> singleFunction = new ArrayList<String>();
					singleFunction.add(methodSignature);
					singleFunction.add(getFromDataVariable);
//					singleFunction.add(methodVariable);
//					singleFunction.add(schemaVariable );
//					if(datas.has("sms")) singleFunction.add("Function call for SMS");
//					if(datas.has("activitylog")) singleFunction.add("Function call for activitylog");
//					if(datas.has("mail")) singleFunction.add("Function call for mail");
					allFunctions.add(singleFunction);
				}
				if (datas.has("DELETE")) {
					String methodSignature = "\tpublic String " + aliasName.replaceAll("/", "_")
							+ "_delete(String primaryid,String where,boolean isdeleteall){";
					String ColumnNameVariable = "\tString columnName=\""
							+ datas.getJSONObject("primarykey").getString("columnname") + "\";";
//					String schemaVariable ="\tString schema = "+"\""+datas.getString("schema")+"\""+";";
					String deleteFromDataVariable = "\tString deleteFromData = " + "\""
							+ datas.optString("DELETE", null) + "\";";
					ArrayList<String> singleFunction = new ArrayList<String>();
					singleFunction.add(methodSignature);
//					singleFunction.add(schemaVariable );
					singleFunction.add(deleteFromDataVariable);
					singleFunction.add(ColumnNameVariable);
//					if(datas.has("sms")) singleFunction.add("Function call for SMS");
//					if(datas.has("activitylog")) singleFunction.add("Function call for activitylog");
//					if(datas.has("mail")) singleFunction.add("Function call for mail");
					allFunctions.add(singleFunction);
				}

			}
		}
		fileWriter.writeOnFormsService(allFunctions,anotherLogic);

	}
}
