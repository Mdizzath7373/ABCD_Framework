package com.eit.abcdframework.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.eit.abcdframework.globalhandler.GlobalAttributeHandler;
import com.eit.abcdframework.globalhandler.GlobalExceptionHandler;
import com.eit.abcdframework.http.caller.Httpclientcaller;
import com.eit.abcdframework.serverbo.CommonServices;
import com.eit.abcdframework.serverbo.DisplaySingleton;
import com.eit.abcdframework.serverbo.ResponcesHandling;
import com.eit.abcdframework.util.AmazonSMTPMail;
import com.eit.abcdframework.util.TimeZoneServices;
import com.eit.abcdframework.websocket.WebSocketService;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Iterator;

@Service
public class FormdataServiceImpl implements FormdataService {

	@Autowired
	Httpclientcaller dataTransmit;

	@Autowired
	AmazonSMTPMail amazonSMTPMail;

	@Autowired
	WebSocketService socketService;

	@Autowired
	PasswordEncoder encoder;

	@Autowired
	ResponcesHandling responcesHandling;

	private static final Logger LOGGER = LoggerFactory.getLogger("DCDesignDataServiceImpl");

	@Override
	public String transmittingToMethod(String method, String data, String which, String preRes) {
		JSONObject displayConfig;
		JSONObject jsonObject1 = null;
		String res = "";
		String councurrentAPIres = "";

		try {
			JSONObject jsonheader = null;
			JSONObject jsonbody = null;
			JSONObject bodyData = null;
			boolean synapi = false;

			JSONObject jsonObjectdata = new JSONObject(data);
			if (jsonObjectdata.has("data"))
				jsonObject1 = new JSONObject(CommonServices.decrypt(jsonObjectdata.getString("data")));
			else
				jsonObject1 = jsonObjectdata;

			if (which.equalsIgnoreCase("")) {

				synapi = jsonObject1.has("PrimaryBody") ? true : false;

				jsonheader = jsonObject1.has("PrimaryBody")
						? new JSONObject(jsonObject1.getJSONObject("PrimaryBody").getJSONObject("header").toString())
						: new JSONObject(jsonObject1.getJSONObject("header").toString());
				jsonbody = jsonObject1.has("PrimaryBody")
						? new JSONObject(jsonObject1.getJSONObject("PrimaryBody").getJSONObject("body").toString())
						: new JSONObject(jsonObject1.getJSONObject("body").toString());
			} else {
				synapi = jsonObject1.has("PrimaryBody") ? true : false;
				jsonheader = jsonObject1.has(which)
						? new JSONObject(jsonObject1.getJSONObject(which).getJSONObject("header").toString())
						: new JSONObject(jsonObject1.getJSONObject("header").toString());
				jsonbody = jsonObject1.has(which)
						? new JSONObject(jsonObject1.getJSONObject(which).getJSONObject("body").toString())
						: new JSONObject(jsonObject1.getJSONObject("body").toString());

			}

			LOGGER.info("Fetching data from Config");
			String displayAlias = jsonheader.getString("name");
			displayConfig = DisplaySingleton.memoryDispObjs2.getJSONObject(displayAlias);
			JSONObject gettabledata = new JSONObject(displayConfig.get("datas").toString());

			// additionalcolumninsert
			LOGGER.info("preRes: "+preRes);
			if (preRes.startsWith("[")) {

				JSONArray datavalues = new JSONArray(preRes);
				if (!preRes.isEmpty()) {
					JSONObject getValues = new JSONObject(datavalues.get(0).toString());
					LOGGER.info("datas:"+gettabledata);
					JSONArray columnArray = gettabledata.getJSONObject("additionalcolumn").getJSONArray("column");

					JSONArray keyArray = gettabledata.getJSONObject("additionalcolumn").getJSONArray("key");

					for (int i = 0; i < columnArray.length(); i++) {
					
						jsonbody.put(keyArray.get(i).toString(), getValues.get(columnArray.get(i).toString()));

					}

				}

			}

			// Check user has Valid or Not
			if (gettabledata.has("checkAPI")) {
				JSONObject checkingFunc = new JSONObject(gettabledata.get("checkAPI").toString());

				if (checkingFunc.getJSONArray("curd").toList().contains(method)) {
					String valueOF = new JSONObject(
							dataTransmit.transmitDataspgrest(GlobalAttributeHandler.getPgrestURL()
									+ checkingFunc.getString("FindTable") + jsonheader.get("UniqueColumn"),
									gettabledata.getString("schema")).get(0).toString())
							.getString("FetchColumn");

					if (valueOF.equalsIgnoreCase("MatchBy")) {
						return new JSONObject()
								.put(GlobalAttributeHandler.getError(), checkingFunc.getString("Message")).toString();
					}
				}

			}

			// Map to body Json
			jsonbody = mappingJson(gettabledata, jsonbody);

			boolean function = jsonheader.has("function") ? jsonheader.getBoolean("function") : false;

			if (gettabledata.has("expectedColumn")) {
				bodyData = new JSONObject(jsonbody.toString());
				jsonbody.remove(gettabledata.getString("expectedColumn"));

			} else {
				bodyData = new JSONObject(jsonbody.toString());
			}

			if (method.equalsIgnoreCase("POST")) {
				LOGGER.info("Enter into POST Method process");
				res = transmittingDatatopgrestpost(gettabledata, jsonbody, function, jsonheader);
			} else {
				LOGGER.info("Enter into PUT Method process");
				res = transmittingDatatopgrestput(gettabledata, jsonbody, function, jsonheader);

			}
			
			if (res.equalsIgnoreCase("PrimaryKey is Missing")) {
				LOGGER.error("Responce Failure :::{}", res);
				return new JSONObject().put(GlobalExceptionHandler.getError(), GlobalExceptionHandler.missingPrimaryKey())
						.toString();
			}
			
			LOGGER.info("RES", res);
		
			if (res != null && res.trim().startsWith("[")) {
			    JSONArray resArray = new JSONArray(res);
			    if(jsonheader.has("getResponse")&&jsonheader.getBoolean("getResponse")) {
			    	if(!resArray.getJSONObject(0).has(GlobalExceptionHandler.getError())) {
			    		return new JSONObject().put(GlobalAttributeHandler.getReflex(), resArray.get(0)).toString();
			    	}
			    }
			    if (!resArray.isEmpty() && resArray.getJSONObject(0).has(GlobalAttributeHandler.getError())) {
			        LOGGER.error("Response Failure ::: {}", res);
			        return resArray.getJSONObject(0).toString();
			    }
			}
			

			LOGGER.info("Success, Enter into Responce Handle Method");
			responcesHandling.curdMethodResponceHandle(res, bodyData, jsonheader, gettabledata, method,
					new ArrayList<>());

			if (gettabledata.has("synchronizedCurdOperation") && synapi) {
				JSONArray typeOfMehods = gettabledata.getJSONObject("synchronizedCurdOperation").getJSONArray("type");
				for (int typeOfMehod = 0; typeOfMehod < typeOfMehods.length(); typeOfMehod++) {
					if (typeOfMehods.get(typeOfMehod).toString().equalsIgnoreCase("Map")) {
						if (jsonheader.has("primaryvalue") && jsonheader.getBoolean("primaryvalue")) {
							councurrentAPIres = CommonServices.mappedCurdOperation2(gettabledata, data, res);
						} else
							councurrentAPIres = CommonServices.mappedCurdOperation2(gettabledata, data, "");

						LOGGER.info("Councurrent API Response----->{}", councurrentAPIres);
					}
				}
 
			}

			if (councurrentAPIres.equalsIgnoreCase("Success")) {
				return new JSONObject().put(GlobalAttributeHandler.getReflex(), GlobalAttributeHandler.getSuccess())
						.toString();
			}

		} catch (Exception e) {
			LOGGER.error(Thread.currentThread().getStackTrace()[0].getMethodName(), e);
			return new JSONObject().put(GlobalExceptionHandler.getError(), e.getMessage()).toString();
		}

		return new JSONObject().put(GlobalAttributeHandler.getReflex(), GlobalAttributeHandler.getSuccess()).toString();
	}

	private JSONObject mappingJson(JSONObject gettabledata, JSONObject jsonbody) {

		if (gettabledata.has("passwordencode") && gettabledata.getBoolean("passwordencode")) {
			if (jsonbody.has(gettabledata.getString("passwordcolumn"))
					&& !jsonbody.getString(gettabledata.getString("passwordcolumn")).equals("")) {
				jsonbody.put(gettabledata.getString("passwordcolumn"),
						encoder.encode(jsonbody.getString(gettabledata.getString("passwordcolumn"))));
			}
		}

		if (gettabledata.has("dateandtime")) {
			jsonbody.put(gettabledata.getString("dateandtime"),
					TimeZoneServices.getDateInTimeZoneforSKT("Asia/Riyadh"));
		}

		return jsonbody;

	}

	private String transmittingDatatopgrestpost(JSONObject gettabledata, JSONObject jsonbody, boolean function,
			JSONObject jsonheader) {
		String response = "";
		try {

			String url = (GlobalAttributeHandler.getPgrestURL() + gettabledata.getString("POST")).replaceAll(" ",
					"%20");
			response = dataTransmit.transmitDataspgrestpost(url, jsonbody.toString(),
					jsonheader.has("primaryvalue") ? jsonheader.getBoolean("primaryvalue") : false,
					gettabledata.getString("schema"));

		} catch (Exception e) {
			LOGGER.error("Exception at {} ", Thread.currentThread().getStackTrace()[1].getMethodName(), e);
			return new JSONArray().put(new JSONObject().put( GlobalExceptionHandler.getError(),e.getMessage())).toString();

		}
		return response;
	}

	private String transmittingDatatopgrestput(JSONObject gettabledata, JSONObject jsonbody, boolean function,
			JSONObject jsonheader) {
		String response = "";
		try {
			String primarykey = gettabledata.getJSONObject(GlobalAttributeHandler.getKey())
					.getString(GlobalAttributeHandler.getPrimarycolumnkey());

			if (jsonbody.has(primarykey) && !jsonbody.get(primarykey).toString().equalsIgnoreCase("")) {
				String url = GlobalAttributeHandler.getPgrestURL() + gettabledata.getString("PUT") + "?" + primarykey
						+ "=eq." + jsonbody.get(primarykey);
				url = url.replace(" ", "%20");

				response = dataTransmit.transmitDataspgrestput(url, jsonbody.toString(),
						jsonheader.has("primaryvalue") ? jsonheader.getBoolean("primaryvalue") : false,
						gettabledata.getString("schema"));
			} else {
				response = "PrimaryKey is Missing";
			}
		} catch (Exception e) {
			LOGGER.error("Exception at {} ", Thread.currentThread().getStackTrace()[1].getMethodName(), e);
			return new JSONArray().put(new JSONObject().put( GlobalExceptionHandler.getError(),e.getMessage())).toString();
		}
		return response;
	}

	@Override
	public String transmittingToMethod(String method, String name, String primary, String where, boolean isdeleteall,
			boolean isbulk) {
		String res = "";
		JSONObject displayConfig;
		try {

			if (!name.matches("[a-zA-Z/_]+")) {
				name = CommonServices.decrypt(name);
				if (!primary.equalsIgnoreCase(""))
					primary = CommonServices.decrypt(primary);
				if (!where.equalsIgnoreCase(""))
					where = CommonServices.decrypt(where);
			}

			displayConfig = DisplaySingleton.memoryDispObjs2.getJSONObject(name);
			JSONObject gettabledata = new JSONObject(displayConfig.get("datas").toString());
			String columnprimarykey = gettabledata.getJSONObject(GlobalAttributeHandler.getKey())
					.getString(GlobalAttributeHandler.getPrimarycolumnkey());
			if (method.equalsIgnoreCase("GET")) {
				res = transmittingDatapgrestget(columnprimarykey, method, gettabledata, primary, where, isbulk);
			} else {
				res = transmittingDatapgrestDel(columnprimarykey, method, gettabledata, primary, where, isdeleteall,
						isbulk);
			}

		} catch (Exception e) {
			LOGGER.error("Exception at {} ", Thread.currentThread().getStackTrace()[1].getMethodName(), e);
			return new JSONObject().put(GlobalExceptionHandler.getError(), e.getMessage()).toString();
		}

		return res;

	}
	
	public String transmittingToMethodDisassociate(String data) {
		JSONObject displayConfig;
		String url = "";
		JSONObject payLoad = new JSONObject(data);
		JSONObject jsonHeader = payLoad.getJSONObject("header");
		String method = jsonHeader.getString("method");
//		JSONArray jsonBody = payLoad.getJSONArray("body");
		String where = jsonHeader.getString("where");
		
		try {
		displayConfig =DisplaySingleton.memoryDispObjs2.getJSONObject(jsonHeader.getString("name"));
	    JSONObject gettabledata = new JSONObject(displayConfig.get("datas").toString());
	    if(method.equalsIgnoreCase("delete")) {
	    	if(gettabledata.has(method)&&gettabledata.get(method) instanceof JSONArray) {
	    		JSONArray delArray = gettabledata.getJSONArray(method);
	    		if(delArray!=null && delArray.length()!=0) {
	    		JSONArray columns = gettabledata.getJSONArray("columns");
	    		for(int i=0;i<delArray.length();i++) {
	    			url = (GlobalAttributeHandler.getPgrestURL() + delArray.get(i)+ "?" + columns.get(i) + "=eq." + where );
//	    			LOGGER.info("DISASSOCIATE URL :"+ url);
	    			dataTransmit.transmitDataspgrestDel(url, gettabledata.getString("schema"));
	    		}
	    		}
	    	}
	    }
	}
		catch(Exception e) {
			LOGGER.error("Exception at {} ", Thread.currentThread().getStackTrace()[1].getMethodName(), e);
			return new JSONObject().put(GlobalExceptionHandler.getError(), e.getMessage()).toString();
		}
		return new JSONObject().put(GlobalAttributeHandler.getReflex(), GlobalAttributeHandler.getSuccess()).toString();
	}

	private String transmittingDatapgrestget(String columnprimarykey, String method, JSONObject gettabledata,
			String primary, String where, boolean isgetbulk) {
		JSONObject returndata = new JSONObject();
		JSONArray temparay;
		try {
			String url = "";
			String which = gettabledata.has("method") ? gettabledata.getString("method") : "GET";
			if (gettabledata.getString(method).startsWith("rpc")&& gettabledata.has("preDefined") && gettabledata.getBoolean("preDefined")) {
				String data = "";

				JSONObject Query = gettabledata.getJSONObject("Query");

				where = where.replace("?datas=", "");
				if (Query.has("where")&&!Query.getString("where").isEmpty()&&!where.equalsIgnoreCase("")) {
					Query.put("where", Query.get("where")+" and "+where+" ");
					data = getOrderedJSONObject(Query);
					
				} else if (!where.equalsIgnoreCase("")) {
					Query.put("where", " WHERE "+where+" ");
					data = getOrderedJSONObject(Query);
				} 
				else{
					data = getOrderedJSONObject(Query);
				}
				url = GlobalAttributeHandler.getPgrestURL() + gettabledata.getString("Function") + "?basequery=" +data ;
		
			} else if (primary != null && !primary.equalsIgnoreCase("")) {
				if (isgetbulk)
					url = (GlobalAttributeHandler.getPgrestURL() + gettabledata.getString(method.toUpperCase()) + "?"
							+ columnprimarykey + "=in.(" + primary + ")");
				else
					url = (GlobalAttributeHandler.getPgrestURL() + gettabledata.getString(method.toUpperCase()) + "?"
							+ columnprimarykey + "=eq." + primary);
			} else if (primary != null && primary.equalsIgnoreCase("") && !where.equalsIgnoreCase("")) {
				url = GlobalAttributeHandler.getPgrestURL() + gettabledata.getString(method.toUpperCase()) + where;
			} else {
				url = GlobalAttributeHandler.getPgrestURL() + gettabledata.getString("GET");
			}

			if (which.equalsIgnoreCase("post")) {
				temparay = new JSONArray();
				JSONObject json = null;
				JSONObject getdata = null;
				if (url.split("\\?").length > 1) {
					String data = url.split("\\?")[1];
					json = new JSONObject();
					String[] arraydata = data.split("&");
					for (int i = 0; i < arraydata.length; i++) {
						System.out.print(arraydata[i]);
						json.put(arraydata[i].split("=")[0], arraydata[i].split("=")[1]);
					}
					url = url.split("\\?")[0];
				} else {
					json = new JSONObject();
				}
				getdata = new JSONObject(new JSONArray(dataTransmit.transmitDataspgrestpost(url, json.toString(), false,
						gettabledata.getString("schema"))).get(0).toString());
				returndata.put(GlobalAttributeHandler.getDatavalue(), temparay.put(getdata));
			} else {
				temparay = dataTransmit.transmitDataspgrest(url, gettabledata.getString("schema"));
				LOGGER.info("temparay "+temparay.toString());
				
				if(temparay.length() > 0 &&temparay.getJSONObject(0).has(GlobalAttributeHandler.getError())) {
					 return temparay.get(0).toString();
				 } 
				returndata.put(GlobalAttributeHandler.getDatavalue(), temparay);
			}

		} catch (Exception e) {
			LOGGER.error("Exception at {}", Thread.currentThread().getStackTrace()[1].getMethodName(), e);
			return new JSONObject().put(GlobalExceptionHandler.getError(), e.getMessage()).toString();

		}	
		return returndata.toString();
	}

	 public static String getOrderedJSONObject(JSONObject input) {
		 String[] keysInOrder = { "query", "where", "groupby", "having", "orderby", "limit" };
		    LinkedHashMap<String, Object> orderedMap = new LinkedHashMap<>();

		    for (String key : keysInOrder) {
		        if (input.has(key)) {
		            orderedMap.put(key, input.get(key));
		        }
		    }
		    try {
		        ObjectMapper objectMapper = new ObjectMapper();
		        String orderedJson = objectMapper.writeValueAsString(orderedMap);
		        return orderedJson;
		    } catch (Exception e) {
		        e.printStackTrace();
		        return null;
		    }
	 }
	
	private String transmittingDatapgrestDel(String columnprimarykey, String method, JSONObject gettabledata,
			String primary, String where, boolean isdeleteall, boolean isdeletebulk) {
		JSONObject returndata = new JSONObject();
		String response = "";
		LOGGER.info(" primary: "+primary);
		LOGGER.info(" where: "+where);

		try {
			String url = "";
			if (primary != null && !primary.equalsIgnoreCase("")) {
				if (!isdeleteall && isdeletebulk)
					url = (GlobalAttributeHandler.getPgrestURL() + gettabledata.getString(method.toUpperCase()) + "?"
							+ columnprimarykey + "=in.(" + primary + ")");
				else
					url = (GlobalAttributeHandler.getPgrestURL() + gettabledata.getString(method.toUpperCase()) + "?"
							+ columnprimarykey + "=eq." + primary);
			} else if (primary != null && primary.equalsIgnoreCase("") && !where.equalsIgnoreCase("")) {
				url = (GlobalAttributeHandler.getPgrestURL() + gettabledata.getString(method.toUpperCase()) + where);

			} else if (isdeleteall) {
				url = url + gettabledata.getString("api");
			} else {
				return returndata.put(GlobalAttributeHandler.getError(), "Please check the data").toString();
			}
			LOGGER.info(" del url: "+url);
			response = dataTransmit.transmitDataspgrestDel(url, gettabledata.getString("schema"));

		} catch (Exception e) {
			returndata.put(GlobalAttributeHandler.getError(), GlobalAttributeHandler.getFailure());
			LOGGER.error("Exception at {}", Thread.currentThread().getStackTrace()[1].getMethodName(), e);
		}
		return new JSONArray(response).get(0).toString();
	}

	
	@Override
	public String transmittingToMethodBulk(String method, String data) {
		JSONObject displayConfig;
		JSONObject jsonObject1 = null;
		String res = "";

		try {
			JSONObject jsonheader = null;
			JSONArray jsonbody = null;

			JSONObject jsonObjectdata = new JSONObject(data);
			if (jsonObjectdata.has("data"))
				jsonObject1 = new JSONObject(CommonServices.decrypt(jsonObjectdata.getString("data")));
			else
				jsonObject1 = jsonObjectdata;

			jsonheader = new JSONObject(jsonObject1.getJSONObject("header").toString());
			jsonbody = new JSONArray(jsonObject1.getJSONArray("body").toString());

			String displayAlias = jsonheader.getString("name");
			displayConfig = DisplaySingleton.memoryDispObjs2.getJSONObject(displayAlias);
			JSONObject gettabledata = new JSONObject(displayConfig.get("datas").toString());

			if (method.equalsIgnoreCase("POST")) {
				res = transmittingDatatopgrestpostBulk(gettabledata, jsonbody, jsonheader);
			} else {
				res = transmittingDatatopgrestputBulk(gettabledata, jsonbody, jsonheader);

			}
			
			if (res != null && res.trim().startsWith("[")) {
			    JSONArray resArray = new JSONArray(res);
			    if (!resArray.isEmpty() && resArray.getJSONObject(0).has(GlobalAttributeHandler.getError())) {
			        LOGGER.error("Response Failure ::: {}", res);
			        return resArray.getJSONObject(0).toString();
			    }
			}
			
		} catch (Exception e) {
			LOGGER.error(Thread.currentThread().getStackTrace()[0].getMethodName(), e);
			return new JSONObject().put(GlobalExceptionHandler.getError(), e.getMessage()).toString();
		}

		return new JSONObject().put(GlobalAttributeHandler.getReflex(), GlobalAttributeHandler.getSuccess()).toString();
	}
	

	private String transmittingDatatopgrestpostBulk(JSONObject gettabledata, JSONArray jsonbody,
			JSONObject jsonheader) {
		String response = "";
		try {
			String url = (GlobalAttributeHandler.getPgrestURL() + gettabledata.getString("POST")).replaceAll(" ",
					"%20");

			String jsonBodyString = jsonbody.toString();

			response = dataTransmit.transmitDataspgrestpost(url, jsonBodyString,
					jsonheader.has("primaryvalue") ? jsonheader.getBoolean("primaryvalue") : false,
					gettabledata.getString("schema"));

		} catch (Exception e) {
			LOGGER.error("Exception at {} ", Thread.currentThread().getStackTrace()[1].getMethodName(), e);
			return new JSONArray().put(new JSONObject().put( GlobalExceptionHandler.getError(),e.getMessage())).toString();
		}
		return response;
	}

	private String transmittingDatatopgrestputBulk(JSONObject gettabledata, JSONArray jsonbody, JSONObject jsonheader) {
		String response = "";
		try {
			String url = (GlobalAttributeHandler.getPgrestURL() + gettabledata.getString("PUT") + "?on_conflict="
					+ gettabledata.getJSONObject("primarykey").getString("columnname")).replaceAll(" ", "%20");

			String jsonBodyString = jsonbody.toString();

			response = dataTransmit.transmitDataspgrestPutbulk(url, jsonBodyString,
					jsonheader.has("primaryvalue") ? jsonheader.getBoolean("primaryvalue") : false,
					gettabledata.getString("schema"));

		} catch (Exception e) {
			LOGGER.error("Exception at {} ", Thread.currentThread().getStackTrace()[1].getMethodName(), e);
			return new JSONArray().put(new JSONObject().put( GlobalExceptionHandler.getError(),e.getMessage())).toString();
		}
		return response;
	}

	@Override
	public String insertViaExcel(MultipartFile file,String data) {
		try {
			String res="";
			JSONObject datas = new JSONObject(data);
			JSONObject datasFromConfigs = new JSONObject(DisplaySingleton.memoryDispObjs2.getJSONObject(datas.getString("name")).get("datas").toString());
			res = transmittingDatatopgrestpostBulk(
					datasFromConfigs,
					convertExcelToJArray(file,datasFromConfigs.getJSONObject("columnNames")),
					datas
					);
			if (res != null && res.trim().startsWith("[")) {
			    JSONArray resArray = new JSONArray(res);
			    if (!resArray.isEmpty() && resArray.getJSONObject(0).has(GlobalAttributeHandler.getError())) {
			        LOGGER.error("Response Failure ::: {}", res);
			        return resArray.getJSONObject(0).toString();
			    }
			}
			
			if(res.equalsIgnoreCase(GlobalExceptionHandler.getError())) {
				return new JSONObject().put(GlobalExceptionHandler.getError(), GlobalExceptionHandler.getUnknownException()).toString();
			}
			
			return new JSONObject().put(GlobalAttributeHandler.getReflex(), GlobalAttributeHandler.getSuccess()).toString();

		}catch(Exception e) {
			e.printStackTrace();
			return new JSONObject().put(GlobalExceptionHandler.getError(), e.getMessage()).toString();

		}
	}
	
private JSONArray convertExcelToJArray(MultipartFile excel,JSONObject columnNames) {
		JSONArray finalResult = new JSONArray();
		
		Workbook xlBook=null;
		
		if(excel.getOriginalFilename().toLowerCase().endsWith(".xlsx")) {
			try {
				xlBook = new XSSFWorkbook(excel.getInputStream());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		else if(excel.getOriginalFilename().toLowerCase().endsWith(".xls")) {
			try {
				xlBook = new HSSFWorkbook(excel.getInputStream());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		else {
			LOGGER.info("Not an Excel file....Returing empty Array");
			return new JSONArray();
			
		}
		
		Sheet sheet = xlBook.getSheetAt(0);
		
		Row headerRow = sheet.getRow(0);
		
		ArrayList<String> headers = new ArrayList<String>();
		
		Iterator<Cell> headerCellIterator = headerRow.cellIterator();
		
		while(headerCellIterator.hasNext()) { // Adding headers in the headers list
			Cell cell = headerCellIterator.next();
			if(cell.getStringCellValue()=="") break;
			headers.add(columnNames.getString(cell.getStringCellValue()));
		}
		
		Iterator<Row> rowIterator = sheet.rowIterator(); // Iterator to iterate over rows
		rowIterator.next(); //Skipping headers, As we already stored in headers list
		
		while(rowIterator.hasNext()) {
			Row row = rowIterator.next();
			
			if(isRowEmpty(row)) continue; //Skipping empty rows
			
			JSONObject json = new JSONObject(); // This will store each rows
			
			for(int i=0;i<headers.size();i++) {
				Cell cell = row.getCell(i);
				Object value = getCellValueWithOriginalType(cell);
				json.put(headers.get(i),value != "" ? value : null);
			}
			finalResult.put(json);
		}
		try {
			xlBook.close();
		} catch (IOException e) {
			e.printStackTrace();
			return new JSONArray (new JSONObject().put(GlobalExceptionHandler.getError(), e.getMessage()));

		}
		
		return finalResult;
}
private boolean isRowEmpty(Row row) {
    if (row == null) {
        return true;
    }
    
    for (int i = row.getFirstCellNum(); i < row.getLastCellNum(); i++) {
        Cell cell = row.getCell(i);
        if (cell != null && cell.getCellType() != CellType.BLANK) {
            // Check if it's a string cell with empty content
            if (cell.getCellType() == CellType.STRING && 
                cell.getStringCellValue().trim().isEmpty()) {
                continue;
            }
            return false;  // Found a non-empty cell
        }
    }
    return true;  // All cells are empty
}

private Object getCellValueWithOriginalType(Cell cell) {
    if (cell == null) {
        return "";
    }
    
    switch (cell.getCellType()) {
        case STRING:
            return cell.getStringCellValue();
        case NUMERIC:
            if (DateUtil.isCellDateFormatted(cell)) {
                return cell.getDateCellValue();
            } else {
                double numericValue = cell.getNumericCellValue();
                // Check if it's actually an integer
                if (numericValue == Math.floor(numericValue) && !Double.isInfinite(numericValue)) {
                    // It's an integer, return as long
                    return (long) numericValue;
                } else {
                    // It's a decimal, return as double
                    return numericValue;
                }
            }
        case BOOLEAN:
            return cell.getBooleanCellValue();
        case BLANK:
            return "";
        case ERROR:
            return "ERROR";
        default:
            return "";
    }
} 
	
//	private StringBuilder URLEncode(String value) {
//		StringBuilder result = new StringBuilder();
//		try {
//			String regex = DisplaySingleton.memoryApplicationSetting.getString("UrlEncodeExcept");
//			for (int i = 0; i < value.length(); i++) {
//				char c = value.charAt(i);
////				System.err.println(c);
//				if (!isArabic(c)) {
//					if (String.valueOf(c).matches(regex)) {
//						// URL encode the special character
//						String encodedChar = URLEncoder.encode(String.valueOf(c), StandardCharsets.UTF_8.toString());
//						result.append(encodedChar);
//					} else {
//						result.append(c);
//					}
//				} else {
//					result.append(c);
//				}
//			}
//		} catch (Exception e) {
//			LOGGER.error("Exception at {}", Thread.currentThread().getStackTrace()[1].getMethodName(), e);
//		}
//		return result;
//	}
//
//	private static boolean isArabic(char c) {
//		return (c >= '\u0600' && c <= '\u06FF');
//	}

}