package com.eit.abcdframework.service;

import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.eit.abcdframework.http.caller.Httpclientcaller;
import com.eit.abcdframework.serverbo.DisplaySingleton;

@Service
public class DashboardDataServiceImpl implements DashboardDataService {

	@Autowired
	Httpclientcaller datatransmit;

	@Value("${applicationurl}")
	private String pgrest;

	@Override
	public String handlerOfSocket(String displaytab, boolean Isfirst, String role, String Where) {
		JSONObject displaydata = null;
		JSONObject returnMessage = new JSONObject();
		String getdata = "";
		try {
			displaydata = DisplaySingleton.memoryDispObjs2.getJSONObject("websocket");

			JSONObject getConfigjson = new JSONObject(displaydata.get("discfg").toString());
			JSONObject getdatas = new JSONObject(displaydata.get("datas").toString());

			if (displaydata == null || displaydata.isEmpty())
				return returnMessage.put("error", "Something went wrong,Please Check this?").toString();

			else {
				JSONObject whereCondition = new JSONObject(Where);
				getdata = getSocketData(getdatas, getConfigjson, displaytab, whereCondition);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return getdata;
	}

	@Override
	public String getSocketData(JSONObject extradatas, JSONObject configdatas, String displaytab,
			JSONObject whereCondition) {
		JSONObject overAllStructure = null;
		JSONArray displayname = null;
		JSONObject isFunction = null;
		try {

			overAllStructure = new JSONObject(configdatas.get("OverAllStructure").toString());
			JSONObject displaytabname = extradatas.getJSONObject(displaytab);
			if (displaytab.equalsIgnoreCase("All")) {
				displayname = extradatas.getJSONObject(displaytab).getJSONArray("alisename");
			} else {
				displayname = new JSONArray().put(extradatas.getJSONObject(displaytab).getString("alisename"));
				overAllStructure = new JSONObject().put(displaytabname.getString(displaytab) + "Data",
						overAllStructure.getJSONArray(displaytabname.getString(displaytab) + "Data").toString());
				System.err.println(overAllStructure);
			}

			isFunction = extradatas.getJSONObject(displaytab).getJSONObject("isFunction");

			for (int i = 0; i < displayname.length(); i++) {
				JSONObject displayObj = new JSONObject(
						DisplaySingleton.memoryDispObjs2.get(displayname.getString(i)).toString());
				String name = isFunction.getBoolean(displayname.getString(i))
						? new JSONObject(displayObj.get("datas").toString()).getString("Function")
						: new JSONObject(displayObj.get("datas").toString()).getString("GET");

				String jsonname = displaytabname.getString(displayname.get(i).toString());
				System.out.println(jsonname);

				String url = "";
				String where = "";
				if (whereCondition.has(displayname.getString(i))
						&& whereCondition.get(displayname.getString(i)).toString().startsWith("{")) {
					JSONObject mulitWhere = new JSONObject(
							whereCondition.get(displayname.get(i).toString()).toString());
					Object[] keys = mulitWhere.keySet().toArray();
					JSONObject jsonObject = new JSONObject();
					for (int k = 0; k < keys.length; k++) {
						url = pgrest + name + mulitWhere.getString(keys[k].toString());
						JSONArray datas = datatransmit.transmitDataspgrest(url,extradatas.getString("schema"));
						JSONArray orignalJson = new JSONObject(configdatas.get(jsonname).toString())
								.getJSONArray("orginalJson");
						JSONArray changedJson = new JSONObject(configdatas.get(jsonname).toString())
								.getJSONArray("changedJson");
						List<Object> arryFormatdatas = configdatas.getJSONArray("ArrayFormatOfStructure").toList();
						JSONArray arrayJson = new JSONArray();
						for (int m = 0; m < datas.length(); m++) {
							JSONObject json = new JSONObject();
							for (int j = 0; j < orignalJson.length(); j++) {
								if (arryFormatdatas.contains(jsonname)) {
									json.put(changedJson.getString(j),
											new JSONObject(datas.get(m).toString()).get(orignalJson.get(j).toString()));

								}
							}
							arrayJson.put(json);
							jsonObject.put(keys[k].toString(), arrayJson);

						}
						if (arryFormatdatas.contains(jsonname) && keys.length == k + 1) {
							overAllStructure.remove(jsonname + "Data");
							overAllStructure.put(jsonname + "Datas", jsonObject);
						}
					}

				} else {
					where = whereCondition.has(displayname.getString(i))
							? whereCondition.getString(displayname.getString(i))
							: "";
//				if (role != null && role.equalsIgnoreCase("Company Admin"))
					if (!where.equalsIgnoreCase(""))
						url = pgrest + name + where;
					else
						url = pgrest + name;

					url = url.replace(" ", "%20");

					JSONArray datas = datatransmit.transmitDataspgrest(url,extradatas.getString("schema"));

					JSONArray orignalJson = new JSONObject(configdatas.get(jsonname).toString())
							.getJSONArray("orginalJson");
					JSONArray changedJson = new JSONObject(configdatas.get(jsonname).toString())
							.getJSONArray("changedJson");
					List<Object> arryFormatdatas = configdatas.getJSONArray("ArrayFormatOfStructure").toList();
					JSONArray arrayJson = new JSONArray();
					for (int m = 0; m < datas.length(); m++) {
						JSONObject json = new JSONObject();
						for (int j = 0; j < orignalJson.length(); j++) {
							if (arryFormatdatas.contains(jsonname)) {
								json.put(changedJson.getString(j),
										new JSONObject(datas.get(m).toString()).get(orignalJson.get(j).toString()));

							} else {
								overAllStructure.put(changedJson.getString(j),
										new JSONObject(datas.get(m).toString()).get(orignalJson.get(j).toString()));
							}
						}
						arrayJson.put(json);

						if (arryFormatdatas.contains(jsonname) && datas.length() == m + 1) {
							overAllStructure.put(jsonname + "Data", arrayJson);
						}
					}
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return overAllStructure.toString();
	}

//	private String ProccessofUpdateSocket(JSONObject extradatas, String displaytab, JSONObject structureJson,
//			boolean Isfirst, JSONArray arryFormatdatas) {
//		JSONObject getLastUpdData = null;
//		JSONObject jsonobject = new JSONObject();
//		try {
//			String pgresturl = DisplaySingleton.memoryApplicationSetting.getString("pgresturl");
//			String displayname = new JSONObject(extradatas.get(displaytab).toString()).getString("displayname");
//			String tablename = new JSONObject(extradatas.get(displaytab).toString()).getString("tablename");
//			String columnname = new JSONObject(extradatas.get(displaytab).toString()).getString("columnname");
//			if (displayname != null && displayname != "") {
//				String url = pgresturl + tablename + "order=" + columnname + ".desc";
//				if (!Isfirst)
//					getLastUpdData = new JSONObject(datatransmit.transmitDataspgrest(url).get(0).toString());
//				else
//					getLastUpdData = new JSONObject(datatransmit.transmitDataspgrest(url).toString());
//
//				if (getLastUpdData == null && getLastUpdData.isEmpty()) {
//					return "Data Is Empty";
//				}
//				JSONArray orignalJson = new JSONArray(structureJson.getJSONArray("orignalJson").toString());
//				JSONArray changedJson = new JSONArray(structureJson.getJSONArray("changedJson").toString());
//				for (int i = 0; i < orignalJson.length(); i++) {
//					if (arryFormatdatas != null) {
//						String data = arryFormatdatas.get(i).toString().equalsIgnoreCase("") ? ""
//								: arryFormatdatas.get(i).toString();
//						if (data == "")
//							jsonobject.put(changedJson.getString(i), getLastUpdData.get(orignalJson.get(i).toString()));
//						else {
//							JSONArray jsonArray = new JSONArray();
//							for (int j = 0; j < getLastUpdData.length(); j++) {
//								jsonobject.put(changedJson.getString(i),
//										getLastUpdData.get(orignalJson.get(j).toString()));
//							}
//
//							return jsonArray.put(jsonobject).toString();
//						}
//
//					} else {
//						jsonobject.put(changedJson.getString(i), getLastUpdData.get(orignalJson.get(i).toString()));
//					}
//
//				}
//			} else {
//				return "Something Missing Please check this";
//			}
//
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//
//		return jsonobject.toString();
//
//	}

}
