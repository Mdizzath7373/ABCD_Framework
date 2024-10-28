package com.eit.abcdframework.websocket;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.eit.abcdframework.globalhandler.GlobalAttributeHandler;
import com.eit.abcdframework.http.caller.Httpclientcaller;
import com.eit.abcdframework.serverbo.DisplaySingleton;
import com.eit.abcdframework.serverbo.FileuploadServices;
import com.eit.abcdframework.service.DashboardDataService;

@Component
public class WebSocketService extends TextWebSocketHandler {

	private static final Logger LOGGER = LoggerFactory.getLogger("WebSocketService");

	private static DashboardDataService dashboardDataService;

	@Autowired
	public void setProductService(DashboardDataService dashboardDataService) {
		this.dashboardDataService = dashboardDataService;
	}

	@Autowired
	Httpclientcaller dataTransmit;

	private static FileuploadServices fileuploadServices;

	@Autowired
	public void setProductService(FileuploadServices fileuploadServices) {
		this.fileuploadServices = fileuploadServices;
	}

	@Autowired
	WebSocketService socketService;

	public static Map<String, List<WebSocketSession>> CompanySession = new HashMap<>();
	public static Map<String, List<WebSocketSession>> AdminSession = new HashMap<>();
	public static Map<String, List<WebSocketSession>> ProgressSessionCA = new HashMap<>();
	public static Map<String, List<WebSocketSession>> ProgressSessionAA = new HashMap<>();
	public static Map<String, JSONObject> lastdata = new HashMap<>();
	public static Map<String, List<WebSocketSession>> RemoveCloseSession = new HashMap<>();

	@Override
	protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
		String data = message.getPayload();
		JSONObject json = new JSONObject(data);
		try {
			String getsocketdata = null;
			if (json.has("type")) {
               LOGGER.info("Ping the connection establised.");
			}
			if (json.has("Isfirst") && json.getBoolean("Isfirst")) {
				getsocketdata = dashboardDataService.handlerOfSocket(json.getString("displaytab"),
						json.getBoolean("Isfirst"), json.getString("push"), json.get("where").toString());
				lastdata.put(json.get("id").toString(), new JSONObject(getsocketdata.toString()));

				if (session.isOpen()) {
					session.sendMessage(new TextMessage(getsocketdata));
				} else {
					LOGGER.warn("Socket Is Not Connected");
				}
			} else {
				if (session.isOpen()) {
					if (json.getString("displaytab").equalsIgnoreCase("progress")) {
						JSONObject messageData = new JSONObject(fileuploadServices.getProgress().entrySet().stream()
								.filter(entry -> json.getJSONObject("where").get("ids").toString()
										.equalsIgnoreCase(entry.getKey().split("-")[0]))
								.collect(Collectors.toMap(entry -> entry.getKey().split("-")[1],
										entry -> entry.getValue().get())));
						session.sendMessage(new TextMessage(messageData.toString()));
					} else
						session.sendMessage(
								new TextMessage(new JSONObject().put("reflx", "Socket Is Connected").toString()));
				} else {
					LOGGER.warn("Socket Is Not Connected");
				}
			}

			if (json.has("displaytab") && json.getString("displaytab").equalsIgnoreCase("progress")) {
				if (json.has("push") && json.getString("push").equalsIgnoreCase("Company Admin")) {

					if (ProgressSessionCA.isEmpty() || !ProgressSessionCA.containsKey(json.get("id").toString())) {
						List<WebSocketSession> newsession = new ArrayList<WebSocketSession>();
						newsession.add(session);
						ProgressSessionCA.put(json.get("id").toString(), newsession);
						LOGGER.warn("Check progress Session is created :: {}",
								ProgressSessionCA.containsKey(json.get("id").toString()));
					} else {
						List<WebSocketSession> jsonArray = ProgressSessionCA.get(json.get("id").toString());
						jsonArray.add(session);
						ProgressSessionCA.put(json.get("id").toString(), jsonArray);
						LOGGER.warn("New progress session Updatedby company {}",
								ProgressSessionCA.get(json.get("id").toString()).toArray().length);
					}
				} else if (json.has("push") && (json.getString("push").equalsIgnoreCase("Airport Officer")
						|| json.getString("push").equalsIgnoreCase("Airport Admin"))) {
					if (ProgressSessionAA.isEmpty() || !ProgressSessionAA.containsKey(json.get("id").toString())) {
						List<WebSocketSession> newsession = new ArrayList<WebSocketSession>();
						newsession.add(session);
						ProgressSessionAA.put(json.get("id").toString(), newsession);
						LOGGER.warn("Check progress Session is created :: {}",
								ProgressSessionAA.containsKey(json.get("id").toString()));
					} else {
						List<WebSocketSession> jsonArray = ProgressSessionAA.get(json.get("id").toString());
						jsonArray.add(session);
						ProgressSessionAA.put(json.get("id").toString(), jsonArray);
						LOGGER.warn("New progress session Updatedby company {}",
								ProgressSessionAA.get(json.get("id").toString()).toArray().length);
					}
				}

			} else {
				if (json.has("push") && json.getString("push").equalsIgnoreCase("Company Admin")) {
					if (CompanySession.isEmpty() || !CompanySession.containsKey(json.get("id").toString())) {
						List<WebSocketSession> newsession = new ArrayList<WebSocketSession>();
						newsession.add(session);
						CompanySession.put(json.get("id").toString(), newsession);
						LOGGER.warn("Check Session is created :: {}",
								CompanySession.containsKey(json.get("id").toString()));
					} else {
						List<WebSocketSession> jsonArray = CompanySession.get(json.get("id").toString());
						jsonArray.add(session);
						CompanySession.put(json.get("id").toString(), jsonArray);
						LOGGER.warn("New session Updatedby company {}",
								CompanySession.get(json.get("id").toString()).toArray().length);

					}
				} else if (json.has("push") && (json.getString("push").equalsIgnoreCase("Airport Officer")
						|| json.getString("push").equalsIgnoreCase("Airport Admin"))) {
					if (AdminSession.isEmpty() || !AdminSession.containsKey(json.get("id").toString())) {
						List<WebSocketSession> newsession = new ArrayList<WebSocketSession>();
						newsession.add(session);
						AdminSession.put(json.get("id").toString(), newsession);
						LOGGER.warn("Check Session is created :: {}",
								AdminSession.containsKey(json.get("id").toString()));
					} else {
						List<WebSocketSession> jsonArray = AdminSession.get(json.get("id").toString());
						jsonArray.add(session);
						AdminSession.put(json.get("id").toString(), jsonArray);
						LOGGER.warn("New session created Updatedby Admin {}",
								AdminSession.get(json.get("id").toString()).toArray().length);
					}
				}
			}
		} catch (Exception e) {
			LOGGER.error("session connection ", e);
		}
	}

	@Override
	public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
		// Log the error or perform necessary error handling
		System.err.println("Error in WebSocket session: " + exception.getMessage());

		// Close the WebSocket session if needed
		if (session.isOpen()) {
			session.close(CloseStatus.SERVER_ERROR);
		}
	}

	public void sendsession(WebSocketSession session, String getsocketdata, String checkSession, String key) {
		try {
			if (session.isOpen()) {
				session.sendMessage(new TextMessage(getsocketdata));
				LOGGER.error("Push Session");
			} else {
				if (RemoveCloseSession.isEmpty() && !RemoveCloseSession.containsKey(key)) {
					List<WebSocketSession> datas = new ArrayList<WebSocketSession>();
					datas.add(session);
					RemoveCloseSession.put(key, datas);
				} else {
					if (RemoveCloseSession.get(key) == null) {
						List<WebSocketSession> datas = new ArrayList<WebSocketSession>();
						datas.add(session);
						RemoveCloseSession.put(key, datas);
					} else {
						List<WebSocketSession> datas = RemoveCloseSession.get(key);
						datas.add(session);
						RemoveCloseSession.put(key, datas);
					}
				}
			}
		} catch (Exception e) {
			LOGGER.error("Send Message", e);
		}
	}

	public String pushSocketData(JSONObject jsonObject, JSONObject jsonbody, String method) {
		JSONArray orignalJson = null;
		JSONArray changedJson = null;
		JSONObject datavalues = null;
		String returnRes = "Success";
		try {
			LOGGER.error("Enter Into pushSocketData Method!");
			if (method.equalsIgnoreCase("progress")) {
				if (ProgressSessionCA.isEmpty() && ProgressSessionAA.isEmpty()) {
//					LOGGER.warn("Socket Connection is empty");
					return returnRes = "Socket is Not Connected,Connection is empty";
				}
				JSONObject returnMes = new JSONObject(fileuploadServices.getProgress().entrySet().stream()
						.filter(entry -> jsonbody.get("ids").toString().equalsIgnoreCase(entry.getKey().split("-")[0]))
						.collect(Collectors.toMap(entry -> entry.getKey().split("-")[1],
								entry -> entry.getValue().get())));

				if (ProgressSessionCA.containsKey(jsonbody.get("ids").toString())) {
					LOGGER.warn("Enter into Progress Session  {}",
							ProgressSessionCA.containsKey(jsonbody.get("ids").toString()));
					List<WebSocketSession> arrayOfSession = ProgressSessionCA.get(jsonbody.get("ids").toString());
					for (int i = 0; i < arrayOfSession.size(); i++) {
						sendsession(arrayOfSession.get(i), returnMes.toString(), "progress",
								jsonbody.get("ids").toString());
					}
				} else if (ProgressSessionAA.containsKey(jsonbody.get("ids").toString())) {
					LOGGER.warn("Enter into Progress Session  {}",
							ProgressSessionAA.containsKey(jsonbody.get("ids").toString()));
					List<WebSocketSession> arrayOfSession = ProgressSessionAA.get(jsonbody.get("ids").toString());
					for (int i = 0; i < arrayOfSession.size(); i++) {
						sendsession(arrayOfSession.get(i), returnMes.toString(), "progress",
								jsonbody.get("ids").toString());
					}
				}

			}

			else {
				if (CompanySession.isEmpty() && AdminSession.isEmpty()) {
//					LOGGER.warn("Socket Connection is empty");
					return returnRes = "Socket is Not Connected,Connection is empty";
				}

				JSONObject returnMessage = new JSONObject();

				JSONObject displaydata = DisplaySingleton.memoryDispObjs2.getJSONObject("websocket");

				JSONObject getConfigjson = new JSONObject(displaydata.get("discfg").toString());
				JSONObject getdatas = new JSONObject(displaydata.get("datas").toString());
				JSONArray getalisname = getdatas.getJSONObject("DataPushOfSocket").getJSONArray("alisename");
				JSONArray getaction = getdatas.getJSONObject("DataPushOfSocket").getJSONArray("Action");
				JSONObject dataof = new JSONObject();
				dataof.put("addcomments", "RecentComments");
				dataof.put("activitylogs", "ActivityLog");

				JSONObject getrefreshdata = new JSONObject();
				getrefreshdata.put("companyrequest", "");

				String name = dataof.has(jsonObject.getString("name")) ? dataof.getString(jsonObject.getString("name"))
						: getaction.toList().contains(jsonObject.getString("name")) ? "OverViewOfDashboard" : "";
				if (!name.equals("")) {
					String role = jsonObject.getString("rolename");
					if (name.equalsIgnoreCase("OverViewOfDashboard")) {
						String url = "";
						if (role.equalsIgnoreCase("Company Admin")) {
							url = GlobalAttributeHandler.getPgrestURL() + "rpc/overviewofdashboard?datas=ids="
									+ jsonbody.get("ids");
						} else {
							url = GlobalAttributeHandler.getPgrestURL() + "rpc/overviewofdashboard";
						}
						datavalues = new JSONObject(
								dataTransmit.transmitDataspgrest(url, getdatas.getString("schema")).get(0).toString());
						datavalues.put("companyname",
								new JSONObject(dataTransmit.transmitDataspgrest(
										GlobalAttributeHandler.getPgrestURL() + "company?id=eq."
												+ jsonbody.get("ids").toString() + "&select=companyname",
										getdatas.getString("schema")).get(0).toString()).getString("companyname"));
						datavalues.put("companyid", jsonbody.get("ids").toString());
						orignalJson = getConfigjson.getJSONObject(name).getJSONArray("push");
						changedJson = getConfigjson.getJSONObject(name).getJSONArray("changedJson");

					} else {
						orignalJson = getConfigjson.getJSONObject(name).getJSONArray("push");
						changedJson = getConfigjson.getJSONObject(name).getJSONArray("changedJson");
						datavalues = new JSONObject(jsonbody.toString());
					}
					if (getalisname.toList().contains(jsonObject.getString("name"))) {
						JSONObject json = new JSONObject();
						for (int i = 0; i < orignalJson.length(); i++) {
							json.put(changedJson.getString(i),
									datavalues.get(orignalJson.get(i).toString()).toString());
						}
						returnMessage.put(name + "Data", new JSONArray().put(json));
					}

					if (role.equalsIgnoreCase("Company Admin")) {
						if (CompanySession.containsKey(jsonbody.get("ids").toString())) {
							LOGGER.warn("Enter into Company Session {}",
									CompanySession.containsKey(jsonbody.get("ids").toString()));
							List<WebSocketSession> arrayOfSession = CompanySession.get(jsonbody.get("ids").toString());
							for (int i = 0; i < arrayOfSession.size(); i++) {
								sendsession(arrayOfSession.get(i), returnMessage.toString(), "company",
										jsonbody.get("ids").toString());
							}
							for (Entry<String, List<WebSocketSession>> data : AdminSession.entrySet()) {
								List<WebSocketSession> arrayOfSession2 = data.getValue();
								for (int i = 0; i < arrayOfSession2.size(); i++) {
									sendsession(arrayOfSession2.get(i), returnMessage.toString(), "Admin",
											data.getKey());
								}
							}
						}
					} else {
						for (Entry<String, List<WebSocketSession>> data : AdminSession.entrySet()) {
							List<WebSocketSession> arrayOfSession = data.getValue();
							for (int i = 0; i < arrayOfSession.size(); i++) {
								sendsession(arrayOfSession.get(i), returnMessage.toString(), "Admin", data.getKey());
							}
						}
						if (CompanySession.containsKey(jsonbody.get("ids").toString())) {
							List<WebSocketSession> arrayOfSession2 = CompanySession.get(jsonbody.get("ids").toString());
							for (int i = 0; i < arrayOfSession2.size(); i++) {
								sendsession(arrayOfSession2.get(i), returnMessage.toString(), "company",
										jsonbody.get("ids").toString());
							}
						}
					}
				}
			}
			if (!RemoveCloseSession.isEmpty()) {
				for (Entry<String, List<WebSocketSession>> data : RemoveCloseSession.entrySet()) {
					String key = data.getKey();
					List<WebSocketSession> sessionsToRemove = data.getValue();

					if (CompanySession.get(data.getKey()) != null) {
						boolean hasCommonElements = sessionsToRemove.stream()
								.anyMatch(CompanySession.get(key)::contains);
						if (hasCommonElements) {
							List<WebSocketSession> arrayofsession = CompanySession.get(data.getKey());
							arrayofsession.removeAll(data.getValue());
							CompanySession.put(data.getKey(), arrayofsession);
							RemoveCloseSession.get(key).removeAll(data.getValue());
						}
					} else if (AdminSession.get(data.getKey()) != null) {
						boolean hasCommonElements = sessionsToRemove.stream().anyMatch(AdminSession.get(key)::contains);
						if (hasCommonElements) {
							List<WebSocketSession> arrayofsession = AdminSession.get(data.getKey());
							arrayofsession.removeAll(data.getValue());
							AdminSession.put(data.getKey(), arrayofsession);
							RemoveCloseSession.get(key).removeAll(data.getValue());
						}
					} else if (ProgressSessionCA.get(key) != null) {
						boolean hasCommonElements = sessionsToRemove.stream()
								.anyMatch(ProgressSessionCA.get(key)::contains);
						if (hasCommonElements) {
							List<WebSocketSession> arrayofsession = ProgressSessionCA.get(data.getKey());
							arrayofsession.removeAll(data.getValue());
							ProgressSessionCA.put(data.getKey(), arrayofsession);
							RemoveCloseSession.get(key).removeAll(data.getValue());
						}
					} else if (ProgressSessionAA.get(data.getKey()) != null) {
						boolean hasCommonElements = sessionsToRemove.stream()
								.anyMatch(ProgressSessionAA.get(key)::contains);
						if (hasCommonElements) {
							List<WebSocketSession> arrayofsession = ProgressSessionAA.get(data.getKey());
							arrayofsession.removeAll(data.getValue());
							ProgressSessionAA.put(data.getKey(), arrayofsession);
							RemoveCloseSession.get(key).removeAll(data.getValue());
						}
					}
					if (RemoveCloseSession.get(key).isEmpty())
						RemoveCloseSession.remove(key);
				}
			}

		} catch (Exception e) {
			returnRes = e.getMessage();
			LOGGER.error("Exception in pushSocketData : ", e);
		}
		return returnRes;
	}

}
