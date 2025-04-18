package com.eit.abcdframework.serverbo;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import com.eit.abcdframework.dto.CommonUtilDto;
import com.eit.abcdframework.globalhandler.GlobalAttributeHandler;
import com.eit.abcdframework.http.caller.Httpclientcaller;

@Service
public class DisplayHandler {

	public static final Logger LOGGER = LoggerFactory.getLogger(DisplayHandler.class);

	@Autowired
	DisplaySingleton displaySingleton;

	@Autowired
	CommonServices commonServices;

	static Httpclientcaller dataTransmits;

	@Autowired
	ApplicationContext context;

	@Autowired
	public void setProductService(@Qualifier("Httpclientcaller") Httpclientcaller service) {
		dataTransmits = service;
	}

//	@Value("${applicationurl}")
//	private String applicationurl;

	@Autowired
	WhereFormation whereFormation;

//	@Autowired
//	public void setProductService(@Qualifier("DCServelt") DCDesignDataServlet services) {
//		dataServlet = services;
//	}

//	public String toExecute(String alias, String mode) {
//		String htmlStr = null, where = null;
//		Connection connection = null;
//		Statement stmt = null;
//		ResultSet rs = null;
//		Configs displayConfig;
//		String subQuery = null;
//
//		try {
//			JSONObject aliesobj = new JSONObject(alias);
//			JSONArray dataBody = aliesobj.getJSONArray("Data");
//			JSONObject whereCondition = (JSONObject) dataBody.get(0);
//
//			where = whereCondition.getString("Where");
//			String displayAlias = whereCondition.getString("Name");
//			if (mode != null && mode.equals("javascript")) {
//				subQuery = whereCondition.getString("SubQuery");
//			}
//			LOGGER.info("display alias = " + displayAlias);
//			displayConfig = DisplaySingleton.memoryDispObjs2.get(displayAlias);
//			LOGGER.info("display object = " + displayConfig);
//			if (displayConfig != null) {
//				JSONObject displayColumns = new JSONObject(displayConfig.getDiscfg());
//				JSONObject extraDatas = new JSONObject(displayConfig.getDatas());
//				JSONObject jsononbj = new JSONObject(displayColumns.getJSONObject("jqxdetails").toString());
//
//				JSONObject columnnames1 = new JSONObject();
//				String linkScreenName = jsononbj.getString("screenname");
//				String type = displayConfig.getDisplaytypes();
//				String gridHeight = "450px";
//				String gridWidth = "1200px";
//				String entity = extraDatas.getString("entity");
//				String api;
//				if (displayConfig.getAttributetypes().equalsIgnoreCase("entity")) {
//					// this entity was api path to send front end
//					api = extraDatas.getString("api");
//				} else {
//					api = extraDatas.getString("api");
//				}
//
//				String displayprimarykey = extraDatas.getJSONObject("primarykey").getString("displayname");
//				if (jsononbj.has("gridheight")) {
//					gridHeight = jsononbj.getString("gridheight");
//				}
//
//				if (jsononbj.has("gridwidth")) {
//					gridWidth = jsononbj.getString("gridwidth");
//				}
//
//				LOGGER.info("JSONOBJECT = " + aliesobj.toString());
//				String queryStr = null;
//				queryStr = displayConfig.getQuery();
//				String datasource = displayConfig.getQuery();
//
//				LOGGER.info("Datasource Jndi Name = " + datasource);
//				/*
//				 * returns the Class object associated with the class or interface with the
//				 * given string name, using the given classloader.
//				 */
//
//				if (!where.isEmpty()) {
//					if (queryStr.contains("WHERE") && where.contains("WHERE")) {
//
//						where = where.replaceAll("WHERE", " AND ");
//
//						queryStr += where;
//					} else if (queryStr.contains("WHERE") && !where.contains("WHERE")) {
//
//						if (subQuery != null && !subQuery.equals("y")) {
//							queryStr += " AND " + where;
//						}
//						queryStr += " WHERE " + where;
//					} else if (!queryStr.contains("WHERE") && where.contains("WHERE")) {
//						queryStr += where;
//					} else if (!queryStr.contains("WHERE") && !where.contains("WHERE")) {
//						queryStr += " WHERE " + where;
//					}
//				}
//				LOGGER.info("stmt = " + stmt);
//				LOGGER.info("query str = " + queryStr);
//				List<Object[]> queryresult = null;
//				if (where != null && where.equalsIgnoreCase("")
//						&& displayConfig.getAttributetypes().equalsIgnoreCase("entity")) {
//					queryresult = resultData.getall(entity);
//					LOGGER.info("Enter into entity method");
//				} else {
//					queryresult = resultData.getresult(queryStr);
//					LOGGER.info("Enter into Query method");
//				}
//				if (queryresult != null && (queryresult.size() == 0 || queryresult.isEmpty())) {
//					commonUtilDtoValue = new CommonUtilDto();
//					List<String> listValues = new ArrayList<String>();
//					listValues.add("No Data");
//					commonUtilDtoValue.setDisplayType(type);
//					commonUtilDtoValue.setPrimarykey(displayprimarykey);
//					commonUtilDtoValue.setJqdetails(jsononbj.toString());
//					commonUtilDtoValue.setFirstRowFilter(jsononbj.getBoolean("firstrowfilter"));
//					commonUtilDtoValue.setLinkscreenname(linkScreenName);
//					commonUtilDtoValue.setGridwidth(gridWidth);
//					commonUtilDtoValue.setGridheight(gridHeight);
//					commonUtilDtoValue.setColumnnames("");
//					commonUtilDtoValue.setEntity(api);
//					JSONArray jsonArray = new JSONArray();
//					commonUtilDtoValue.setDatavalues(jsonArray.toString());
//
//				} else {
//					commonUtilDtoValue = new CommonUtilDto();
//					List<String> listValues1 = new ArrayList<String>();
//					listValues1.add("Datas");
//					commonUtilDtoValue.setFirstRowFilter(jsononbj.getBoolean("firstrowfilter"));
//					commonUtilDtoValue.setLinkscreenname(linkScreenName);
//					commonUtilDtoValue.setPrimarykey(displayprimarykey);
//					commonUtilDtoValue.setDisplayType(type);
//					commonUtilDtoValue.setJqdetails(jsononbj.toString());
//					commonUtilDtoValue.setGridwidth(gridWidth);
//					commonUtilDtoValue.setGridheight(gridHeight);
//					commonUtilDtoValue.setEntity(api);
//					JSONArray jsonArray = new JSONArray();
//					JSONObject jsonObject2;
//					if (queryresult != null && !queryresult.isEmpty() && queryresult.size() != 0) {
//						for (int i = 0; i < queryresult.size(); i++) {
//							jsonObject2 = new JSONObject();
//							JSONArray dataJson = displayColumns.getJSONArray("columns");
//							List<String> listValues = new ArrayList<String>();
//							if (where != null && where.equalsIgnoreCase("")
//									&& displayConfig.getAttributetypes().equalsIgnoreCase("entity")) {
//								ObjectWriter ow = new ObjectMapper().registerModule(new JavaTimeModule())
//										.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS).writer()
//										.withDefaultPrettyPrinter();
//								JSONObject qo = new JSONObject(ow.writeValueAsString(queryresult.get(i)));
//								for (int j = 0; j < qo.length(); j++) {
//									JSONObject dataJsonObject = (JSONObject) dataJson.get(j);
//									listValues.add((qo.isNull(dataJsonObject.getString("columnname"))) ? null
//											: String.valueOf(qo.get(dataJsonObject.getString("columnname"))));
//									jsonObject2.put(dataJsonObject.getString("displayfield"),
//											(qo.isNull(dataJsonObject.getString("columnname"))) ? null
//													: qo.get(dataJsonObject.getString("columnname")) != null
//															? (qo.isNull(dataJsonObject.getString("columnname"))) ? null
//																	: qo.get(dataJsonObject.getString("columnname"))
//															: "");
//									columnnames1.put(dataJsonObject.getString("displayfield"),
//											dataJsonObject.getString("columnname"));
//								}
//								jsonArray.put(jsonObject2);
//
//							} else {
//								Object[] objs = (Object[]) queryresult.get(i);
//								for (int j = 0; j < objs.length; j++) {
//									JSONObject dataJsonObject = (JSONObject) dataJson.get(j);
//									listValues.add(String.valueOf(objs[j]));
//									jsonObject2.put(dataJsonObject.getString("displayfield"),
//											objs[j] != null ? objs[j] : "");
//									columnnames1.put(dataJsonObject.getString("displayfield"),
//											dataJsonObject.getString("columnname"));
//								}
//
//								jsonArray.put(jsonObject2);
//
//							}
//						}
//					}
//					commonUtilDtoValue.setColumnnames(columnnames1.toString());
//					commonUtilDtoValue.setDatavalues(String.valueOf(jsonArray));
//				}
//				if (mode != null && mode.equalsIgnoreCase("javascript")) {
//					CommonUtilDtoServer commonUtilDtoServer = setDataForServerDto(commonUtilDtoValue);
//					htmlStr = new JSONSerializer().exclude("*.class").deepSerialize(commonUtilDtoServer);
//				} else {
//					htmlStr = new JSONSerializer().exclude("*.class").deepSerialize(commonUtilDtoValue);
//				}
//				LOGGER.info("design and data htmlStr = " + htmlStr);
//			}
//
//		} catch (Exception e) {
//			LOGGER.error("Exception in toExecute : ", e);
//		} finally {
//			finallyDesignDataMethod(connection, stmt, rs);
//		}
//		LOGGER.info("design and data htmlStr = " + htmlStr);
//
//		return htmlStr;
//	}

	public CommonUtilDto toExecutePgRest(String alias, boolean function, String role) {
		CommonUtilDto commonUtilDtoValue = new CommonUtilDto();
		String where = null;
		JSONArray res = new JSONArray();
		JSONObject displayConfig;
		String url;
		try {
			List<JSONObject> checkjson = new ArrayList<>();
			JSONObject aliesobj = new JSONObject(alias);
			JSONArray dataBody = aliesobj.getJSONArray("Data");
			JSONObject whereCondition = (JSONObject) dataBody.get(0);

			where = whereCondition.getString("Where");
			String displayAlias = whereCondition.getString("Name");

			LOGGER.info("display alias = {}", displayAlias);
			displayConfig = DisplaySingleton.memoryDispObjs2.getJSONObject(displayAlias);

			LOGGER.info("display object = {}", displayConfig);
			if (displayConfig != null) {
				JSONObject displayColumns = new JSONObject(displayConfig.get("discfg").toString());
				JSONObject extraDatas = new JSONObject(displayConfig.get("datas").toString());
				JSONObject jsononbj = new JSONObject(displayColumns.getJSONObject("jqxdetails").toString());
				JSONObject columnnames1 = new JSONObject();
				String linkScreenName = jsononbj.getString("screenname");
				String type = displayConfig.getString("displaytypes");
				String gridHeight = "450px";
				String gridWidth = "1200px";
				String api = function ? extraDatas.getString("Function") : extraDatas.getString("api");

				String displayprimarykey = extraDatas.getJSONObject("primarykey").getString("displayname");

				if (jsononbj.has("gridheight")) {
					gridHeight = jsononbj.getString("gridheight");
				}

				if (jsononbj.has("gridwidth")) {
					gridWidth = jsononbj.getString("gridwidth");
				}

				LOGGER.info("JSONOBJECT = {}", aliesobj);
				commonUtilDtoValue = new CommonUtilDto();
				List<String> listValues1 = new ArrayList<>();
				listValues1.add("Datas");

				commonUtilDtoValue.setFirstRowFilter(jsononbj.getBoolean("firstrowfilter"));
				commonUtilDtoValue.setLinkscreenname(linkScreenName);
				commonUtilDtoValue.setPrimarykey(displayprimarykey);

				commonUtilDtoValue.setDisplayType(type);

				commonUtilDtoValue.setGridwidth(gridWidth);
				commonUtilDtoValue.setGridheight(gridHeight);
				commonUtilDtoValue.setEntity(api);

				if (function && extraDatas.has("preDefined") && extraDatas.getBoolean("preDefined")) {
					LOGGER.info("Enter into preDefined function");
					JSONObject quryJson = extraDatas.getJSONObject("Query");
					if (quryJson.has("where")) {
						String whereCon = quryJson.getString("where")
								+ (where.equalsIgnoreCase("") ? "" : " and " + where);
						quryJson.put("where", whereCon);
					} else if (!where.equalsIgnoreCase("")) {
						quryJson.put("where", (" WHERE " + where.replace("?datas=", "")));

					}
					url = GlobalAttributeHandler.getPgrestURL() + "rpc/predefine_function" + "?basequery=" + quryJson;
				} else if (function && !where.isEmpty()) {
					LOGGER.info("Enter into function Without where");
					if (extraDatas.has("name"))
						url = GlobalAttributeHandler.getPgrestURL() + api + where + "&" + "name="
								+ extraDatas.getString("name");
					else
						url = GlobalAttributeHandler.getPgrestURL() + api + where;
				} else if (function) {
					LOGGER.info("Enter into function");
					if (extraDatas.has("name"))
						url = GlobalAttributeHandler.getPgrestURL() + api + "?name=" + extraDatas.getString("name");
					else
						url = GlobalAttributeHandler.getPgrestURL() + api + "?datas=";
				} else if (!where.isEmpty()) {
					LOGGER.info("Enter into API Without Where");
					url = GlobalAttributeHandler.getPgrestURL() + api + "?" + where;
				} else {
					LOGGER.info("Enter into API");
					url = GlobalAttributeHandler.getPgrestURL() + api;
				}

				res = dataTransmits.transmitDataspgrest(url, extraDatas.getString("schema"));
//				LOGGER.info("Res = {}", res);

				String key = extraDatas.has("gridDisplayKey")
						&& !extraDatas.getString("gridDisplayKey").equalsIgnoreCase("")
								? extraDatas.getString("gridDisplayKey")
								: "displayfield";

				if (res.length() != 0)
					commonServices.addAdditionalFields(extraDatas, res, jsononbj,
							whereCondition.optBoolean("convertTime"),
							whereCondition.optJSONObject("additionalInformation"));

				if (whereCondition.optBoolean("convertToAddress"))
					commonServices.changeGeoCodeToAddress(res, extraDatas);

				JSONObject jsonObject2;
				JSONArray jsonArray = new JSONArray();

				for (int i = 0; i < res.length(); i++) {
					jsonObject2 = new JSONObject();
					JSONObject getresjson = new JSONObject(res.get(i).toString());
					JSONArray dataJson = jsononbj.getJSONArray("columns");
					for (int j = 0; j < dataJson.length(); j++) {
						JSONObject dataJsonObject = (JSONObject) dataJson.get(j);
						jsonObject2.put(dataJsonObject.getString("displayfield"),
								getresjson.get(dataJsonObject.getString("columnname")));
						columnnames1.put(dataJsonObject.getString("displayfield"),
								dataJsonObject.getString("columnname"));
					}
					jsonArray.put(jsonObject2);
				}
				if (role != null && !role.equalsIgnoreCase("")) {
					JSONArray datas = jsononbj.getJSONArray("columns");
					List<Object> showgirddata = new JSONObject(jsononbj.get("showgridbyrole").toString())
							.getJSONArray(role).toList();
					for (int i = 0; i < datas.length(); i++) {
						if (showgirddata.contains(new JSONObject(datas.get(i).toString()).getString("columnname"))) {
							JSONObject updateObj = new JSONObject(datas.get(i).toString());
							if (!key.equalsIgnoreCase("displayfield")) {
								String value = updateObj.getString("displayfield");
								updateObj.remove("displayfield");
								updateObj.put(key, value);

							}
							checkjson.add(updateObj);
						}

					}

					jsononbj.put("columns", checkjson);

				}
				commonUtilDtoValue.setColumnnames(columnnames1.toString());
				commonUtilDtoValue.setDatavalues(String.valueOf(jsonArray));
				commonUtilDtoValue.setJqdetails(jsononbj.toString());
			}
			LOGGER.info("design and data htmlStr = Data Returned");

		} catch (Exception e) {
			LOGGER.error("Error : ", e);
		}
		return commonUtilDtoValue;
	}

//	public String toExecutePgRest(String alias, boolean function, String role, String chartType) {
//		String response = "null";
//		JSONObject jsonObject4 = new JSONObject();
//		JSONArray jsonArray = new JSONArray();
//		JSONArray colour = new JSONArray();
//		JSONArray lang = new JSONArray();
//		JSONArray displayName = new JSONArray();
//		JSONArray datavalues = new JSONArray();
//		String where = null;
//		JSONArray res = new JSONArray();
//		JSONObject displayConfig;
//		String url;
//		try {
//			JSONObject aliesobj = new JSONObject(alias);
//			JSONArray dataBody = aliesobj.getJSONArray("Data");
//			JSONObject whereCondition = (JSONObject) dataBody.get(0);
//
//			where = whereCondition.getString("Where");
//			String displayAlias = whereCondition.getString("Name");
//
//			LOGGER.info("display alias = {}", displayAlias);
//			displayConfig = DisplaySingleton.memoryDispObjs2.getJSONObject(displayAlias);
//
//			LOGGER.info("display object = {}", displayConfig);
//			if (displayConfig != null) {
//				JSONObject displayColumns = new JSONObject(displayConfig.get("discfg").toString())
//						.getJSONObject("jqxdetails");
//				JSONObject extraDatas = new JSONObject(displayConfig.get("datas").toString());
//
////				JSONObject object = displayColumns.getJSONObject("jqxdetails");
//				String chart = chartType;
//
//				if (displayColumns.has(chart)) {
//					response = new JSONObject(GlobalAttributeHandler.getError(), "Chart Type Dose not exits!")
//							.toString();
//				}
//
//				JSONArray chartConfig = displayColumns.getJSONArray(chart);
//
//				String api = function ? extraDatas.getString("Function") : extraDatas.getString("api");
//
//				String regex = DisplaySingleton.memoryApplicationSetting.getString("UrlEncodeExcept");
//				StringBuilder result = new StringBuilder();
//				for (int i = 0; i < where.length(); i++) {
//					char c = where.charAt(i);
//					if (String.valueOf(c).matches(regex)) {
//						// URL encode the special character
//						String encodedChar = URLEncoder.encode(String.valueOf(c), "UTF-8");
//						result.append(encodedChar);
//					} else {
//						result.append(c);
//					}
//				}
//
//				if (function && extraDatas.has("preDefined") && extraDatas.getBoolean("preDefined")) {
//					JSONObject quryJson = extraDatas.getJSONObject("Query");
//					if (quryJson.has("where")) {
//						String whereCon = quryJson.getString("where")
//								+ (where.equalsIgnoreCase("") ? "" : " and " + where.replace("?datas=", ""));
//						quryJson.put("where", whereCon);
//					}
//					url = GlobalAttributeHandler.getPgrestURL() + extraDatas.getString("Function") + "?basequery="
//							+ extraDatas.getJSONObject("Query");
//				} else if (function && !where.isEmpty()) {
//					if (extraDatas.has("name"))
//						url = GlobalAttributeHandler.getPgrestURL() + api + where + "&" + "name="
//								+ extraDatas.getString("name");
//					else
//						url = GlobalAttributeHandler.getPgrestURL() + api + where;
//				} else if (function) {
//					if (extraDatas.has("name"))
//						url = GlobalAttributeHandler.getPgrestURL() + api + "?name=" + extraDatas.getString("name");
//					else
//						url = GlobalAttributeHandler.getPgrestURL() + api + "?datas=";
//				} else if (!where.isEmpty()) {
//					url = GlobalAttributeHandler.getPgrestURL() + api + "?" + where;
//				} else {
//					url = GlobalAttributeHandler.getPgrestURL() + api;
//				}
//
//				if (extraDatas.has("preDefined") && extraDatas.getBoolean("preDefined")) {
//					res = new JSONObject(new JSONArray(
//							dataTransmits.transmitDataspgrest(url, extraDatas.getString("schema")).get(0).toString()))
//							.getJSONArray("datavalues");
//				} else {
//					res = dataTransmits.transmitDataspgrest(url, extraDatas.getString("schema"));
//				}
//
//				List<Object> showgirddata = new JSONObject(displayColumns.get("showchartbyrole").toString())
//						.getJSONArray(role).toList();
//
//				for (int i = 0; i < res.length(); i++) {
//					JSONObject jsonObject = res.getJSONObject(i);
//					if (showgirddata.contains(jsonObject.get("types"))) {
//						if (chartType.equalsIgnoreCase("barchart")) {
//							JSONObject tempDataObj = new JSONObject();
//							tempDataObj.put("x", jsonObject.get("types"));
//							tempDataObj.put("y",jsonObject.get("counts"));
//							datavalues.put(tempDataObj);
//						} else if (chart.equalsIgnoreCase("piechart") || chart.equalsIgnoreCase("linechart")) {
//							if (jsonObject.get("types")
//									.equals(new JSONObject(chartConfig.get(i).toString()).get("Type"))) {
//
//								JSONObject tempDataObj = new JSONObject();
//								tempDataObj.put("Colour", new JSONArray()
//										.put(new JSONObject(chartConfig.get(i).toString()).get("color")));
//								tempDataObj.put("Lang",
//										new JSONArray().put(new JSONObject(chartConfig.get(i).toString()).get("lang")));
//								tempDataObj.put("Type",
//										new JSONArray().put(new JSONObject(chartConfig.get(i).toString()).get("Type")));
//								tempDataObj.put("Count", new JSONArray().put(jsonObject.get("counts")));
//								datavalues.put(tempDataObj);
//							}
//						}
//					}
//				}
//				response = datavalues.toString();
//
////				if (chartType.equalsIgnoreCase("barchart")) {
//////					List<Object> showgirddata = new JSONObject(object.get("showchartbyrole").toString())
//////							.getJSONArray(role).toList();
////
////					for (int i = 0; i < res.length(); i++) {
////						JSONObject jsonObject = res.getJSONObject(i);
////						if (showgirddata.contains(jsonObject.get("types"))) {
////							JSONObject object5 = new JSONObject();
////							JSONArray jsonArray3 = new JSONArray();
////							jsonArray3.put(0);
////							jsonArray3.put(jsonObject.get("counts"));
////							object5.put("x", jsonObject.get("types"));
////							object5.put("y", jsonArray3);
////							jsonArray.put(object5);
////						}
////					}
////					response = jsonArray.toString();
////				} else if (chart.equalsIgnoreCase("piechart") || chart.equalsIgnoreCase("linechart")) {
////					if (role != null && !role.equalsIgnoreCase("")) {
//////						List<Object> showgirddata = new JSONObject(object.get("showchartbyrole").toString())
//////								.getJSONArray(role).toList();
////						for (int i = 0; i < res.length(); i++) {
////							JSONObject jsonObject = res.getJSONObject(i);
////							if (showgirddata.contains(jsonObject.get("types"))) {
////								for (int j = 0; j < jsonArray2.length(); j++) {
////									JSONObject object2 = new JSONObject(jsonArray2.get(j).toString());
////									if (jsonObject.get("types").equals(object2.get("Type"))
////											&& chart.equalsIgnoreCase("piechart")) {
////										colour.put(object2.get("color"));
////										lang.put(object2.get("lang"));
////										break;
////									}
////								}
////								datavalues.put(jsonObject.get("counts"));
////								displayName.put(jsonObject.get("types"));
////							}
////						}
////						if (chart.equalsIgnoreCase("piechart"))
////							jsonObject4.put("Lang", lang);
////						jsonObject4.put("Colour", colour);
////						jsonObject4.put("Type", displayName);
////						jsonObject4.put("Count", datavalues);
////					}
////					response = jsonObject4.toString();
////				}
//			} else {
//				response = new JSONObject()
//						.put(GlobalAttributeHandler.getError(), "Config has not found, Please Check the Config name!")
//						.toString();
//			}
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//		return response;
//
//	}

	public String toExecutePgRest(String alias, boolean function, String role, String chartType) {
		JSONObject result = new JSONObject(); // final result
		String query = "";
		try {
			JSONArray series = null;
			JSONObject datasJson = new JSONObject(alias); // converting datas into json object
			JSONArray res = new JSONArray(); // it will hold res from db operation

			String aliasName = new JSONObject(datasJson.getJSONArray("Data").get(0).toString()).getString("Name");
			String where = new JSONObject(datasJson.getJSONArray("Data").get(0).toString()).getString("Where");
			JSONObject oneRow = DisplaySingleton.memoryDispObjs2.getJSONObject(aliasName); // it will have particular
																							// row from configs table
			JSONObject discfg = new JSONObject(oneRow.get("discfg").toString());
			JSONObject datasFromConfigs = new JSONObject(oneRow.get("datas").toString());

			String url = GlobalAttributeHandler.getPgrestURL();

			if (datasFromConfigs.getBoolean("preDefineFunction")
					&& !datasFromConfigs.getString("query").equalsIgnoreCase("")) {
				url += "rpc/get_chart_function";
				if (!where.equalsIgnoreCase("")) {
					query = datasFromConfigs.getString("query").replace("wherecondition", where);
				} else {
					if (datasFromConfigs.getString("query").contains("wherecondition"))
						query = datasFromConfigs.getString("query").replace("wherecondition", "");
				}
				url += "?query_text=" + query;
				res = dataTransmits.transmitDataspgrest(url, datasFromConfigs.getString("schema"));

				series = res.length()!=0 ? new JSONObject(res.get(0).toString()).getJSONArray("y"): new JSONArray();

			}

			JSONArray xAxis = null;
			if (datasFromConfigs.has("x") && datasFromConfigs.getBoolean("x")) {
				xAxis = new JSONArray(res.getJSONObject(0).getJSONArray("x").toString());
			} else {
				xAxis = discfg.getJSONArray("xAxis");
			}
			if (chartType.equalsIgnoreCase("donut")) {
				result.put("labels", xAxis);
				result.put("series", series.getJSONObject(0).getJSONArray("datas"));
				result.put("chartType", chartType);
				result.put("colors", discfg.getJSONArray("colors"));
			} else {
				if (xAxis != null) {
					result.put("xAxis", xAxis);
				} else {
					System.out.println("xAxis is null...");
				}
				result.put("series", series);
				result.put("chartType", chartType);
				result.put("colors", discfg.getJSONArray("colors"));
			}

		} catch (Exception e) {
			LOGGER.error(Thread.currentThread().getStackTrace()[0].getMethodName(), e);
		}
		return result.toString();
	}

}