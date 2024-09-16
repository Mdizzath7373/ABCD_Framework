package com.eit.abcdframework.cronservices;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Date;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.eit.abcdframework.http.caller.Httpclientcaller;
import com.eit.abcdframework.serverbo.CommonServices;
import com.eit.abcdframework.serverbo.DisplaySingleton;
import com.eit.abcdframework.util.AmazonSMTPMail;
import com.eit.abcdframework.util.MessageServices;
import com.eit.abcdframework.util.TimeZoneServices;

@Service("cronservice")
public class CronServices {

	@Value("${applicationurl}")
	private String applicationurl;

	@Autowired
	MessageServices messageServices;

	private static final Logger LOGGER = LoggerFactory.getLogger("CronServices");

	public String remainderThroughEmail(String where) {
		String resultOfMail = "";
		String body = "";
		JSONObject smtpMail = new JSONObject(
				DisplaySingleton.memoryApplicationSetting.get("smptAmazonMail").toString());
		try {
			AmazonSMTPMail amazonSMTPMail = new AmazonSMTPMail();
			Httpclientcaller dataTrans = new Httpclientcaller();
			String url = applicationurl + "rpc/getremainderdata?datas=" + where;
			JSONArray json = dataTrans.transmitDataspgrest(url);
			for (int i = 0; i < json.length(); i++) {
				JSONObject jsondata = new JSONObject(json.get(i).toString());
				String subject = "Remainder Of Documents";
				if (where == "milestone") {
					String expiryDate = LocalDate.now().plusMonths(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
					String[] primaryDataParts = jsondata.getString("primarydata").split("\\+");
					String companyName = primaryDataParts[1];
					subject = "Remainder Of Permission";
//					body = "<div style='font-weight:500;font-size:15px'><b>" + jsondata.getString("primarydata")
//							+ "</b><p> Your Milestone Expiry Remainder</p><b><p>We kindly inform you that your </b>"
//							+ jsondata.getString("docsname") + " for the Fleet ID-" + jsondata.get("primarydata")
//							+ " will expire on " + expiryDate + ". Please address this matter promptly.</p></div>";
					body = "<div style='font-family: Arial, sans-serif; color: #333;'>"
						    + "<p>Dear <b>" + companyName + "</b>,</p>"
						    + "<p>This is a reminder that your Milestone document is approaching its expiry date. Please review and update the document as needed.</p>"
						    + "<p><b>Document Name:</b> " + jsondata.getString("docsname") + "</p>"
						    + "<p><b>Expiry Date:</b> " + expiryDate + "</p>"
						    + "<p>Kindly ensure that all necessary updates are made before the expiration date. If you have any questions or need assistance, feel free to reach out.</p>"
						    + "</div>";
				} else if (jsondata.getString("primarydata").equalsIgnoreCase("fleet")) {
//					body = "<div style='font-weight:500;font-size:15px'><b>" + jsondata.getString("primarydata")
//							+ "</b><p> Your Fleet Documents has Expiry Remainder,Please Check Your Documents</p>,<b><p>Document Name:</b>"
//							+ jsondata.getString("docsname") + "-" + jsondata.get("expriydate") + "</p></div>";
					String[] primaryDataParts = jsondata.getString("primarydata").split("\\+");
					String fleetID = primaryDataParts[0];
					String companyName = primaryDataParts[1];
					String expiryDate = primaryDataParts[2];
					body = "<div style='font-family: Arial, sans-serif; color: #333;'>"
						    + "<p>Dear <b>" + companyName + "</b>,</p>"
						    + "<p>This is a reminder that your company document is approaching its expiry date. Please review and update the document as needed.</p>"
						    + "<p><b>Document Name:</b> " + jsondata.getString("docsname") + "</p>"
						    + "<p><b>Expiry Date:</b> " + expiryDate + "</p>"
						    + "<p>Kindly ensure that all necessary updates are made before the expiration date. If you have any questions or need assistance, feel free to reach out.</p>"
						    + "</div>";
				} else {
//					body = "<div style='font-weight:500;font-size:15px'><b>" +"Dear "+ jsondata.getString("primarydata")
//							+ "</b><p> Your Company Documents has Expiry Remainder,Please Check Your Documents</p>,<b><p>Document Name:</b>"
//							+ jsondata.getString("docsname") + "-" + jsondata.get("expriydate") + "</p></div>";
					String[] primaryDataParts = jsondata.getString("primarydata").split("\\+");
					String companyName = primaryDataParts[0];
					String expiryDate = primaryDataParts[1];
					body = "<div style='font-family: Arial, sans-serif; color: #333;'>"
						    + "<p>Dear <b>" + companyName + "</b>,</p>"
						    + "<p>This is a reminder that your company document is approaching its expiry date. Please review and update the document as needed.</p>"
						    + "<p><b>Document Name:</b> " + jsondata.getString("docsname") + "</p>"
						    + "<p><b>Expiry Date:</b> " + expiryDate + "</p>"
						    + "<p>Kindly ensure that all necessary updates are made before the expiration date. If you have any questions or need assistance, feel free to reach out.</p>"
						    + "</div>";

				}
				resultOfMail = amazonSMTPMail.sendEmail(smtpMail.getString("amazonverifiedfromemail"),
						jsondata.getString("email"), subject, body, smtpMail.getString("amazonsmtpusername"),
						smtpMail.getString("amazonsmtppassword"), smtpMail.getString("amazonhostaddress"),
						smtpMail.getString("amazonport"));

			}
		} catch (Exception e) {
			LOGGER.error(Thread.currentThread().getStackTrace()[0].getMethodName(), e);
		}

		return resultOfMail;
	}

	public String triggerCorn() {
		String res = "";
		String sendto = "mobile";
		try {
			Httpclientcaller dataTrans = new Httpclientcaller();
			CommonServices commonServices = new CommonServices();
			String url = applicationurl + "rpc/list_properties_with_expiring_rentals?datas=e.enddate='"+TimeZoneServices.getDate(new Date())+"'";
			JSONArray json = dataTrans.transmitDataspgrest(url);
			if (!json.isEmpty()) {
				for (int i = 0; i < json.length(); i++) {
					JSONObject datavalue = new JSONObject(json.get(i).toString());
					if (sendto.equalsIgnoreCase("mobile")) {
						JSONObject setvalue = new JSONObject();
						setvalue.put("navigatedisplaytab", "renewal");
						setvalue.put("displaytype", "rent_renewal");
						setvalue.put("logs",
								"Dear " + datavalue.get("username") + ", Your contract is going to end by ["
										+ datavalue.get("enddate") + "] for the property  ["
										+ datavalue.get("property_name")
										+ "]. Would you like to \"Renew\" or \"Terminate\" the contract?");
						setvalue.put("ids", datavalue.get("rent_request_id"));
						setvalue.put("data", datavalue.get("user_id"));
						setvalue.put("activitytype", "edit");
						setvalue.put("createdby", "tenant");
						setvalue.put("createdtime", TimeZoneServices.getDateInTimeZoneforSKT("Asia/Riyadh"));
						url = applicationurl + "activitylog";
						dataTrans.transmitDataspgrestpost(url, setvalue.toString(), false);
						res = commonServices.sendPushNotification(setvalue, "activitylog", "tenant",new JSONObject());
					}
					messageServices.MsegatsmsService(datavalue.getString("mobile"),
							"Dear " + datavalue.get("username") + ", Your contract is going to end by ["
									+ datavalue.get("enddate") + "] for the property  ["
									+ datavalue.get("property_name")
									+ "]. Would you like to \"Renew\" or \"Terminate\" the contract?");
				}
			}
		} catch (Exception e) {
			LOGGER.error(Thread.currentThread().getStackTrace()[0].getMethodName(), e);
		}
		return res;
	}

}
