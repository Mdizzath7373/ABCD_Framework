package com.eit.abcdframework.cronservices;

import java.util.ArrayList;
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
import com.eit.abcdframework.serverbo.ResponcesHandling;
import com.eit.abcdframework.util.AmazonSMTPMail;
import com.eit.abcdframework.util.MessageServices;
import com.eit.abcdframework.util.TimeZoneServices;

@Service("cronservice")
public class CronServices {

	@Value("${applicationurl}")
	private String applicationurl;
	
	@Value("${schema}")
	private String schema;

	@Autowired
	AmazonSMTPMail amazonSMTPMail;

	private static final Logger LOGGER = LoggerFactory.getLogger("CronServices");

	public String remainderThroughEmail() {
		String resultOfMail = "";
		try {
			JSONObject jobScheduler = new JSONObject(
					DisplaySingleton.memoryApplicationSetting.get("JobScheduler").toString());
				
				amazonSMTPMail.cornEmialScheduler(jobScheduler.getJSONArray("listOfJob"),jobScheduler.getJSONObject("emailConfig"),jobScheduler.getJSONObject("findby"), new ArrayList<>());
				
		
			
			
			
			
			

//			for (int i = 0; i < listOfJob.length(); i++) {
//				try {
//					String job = listOfJob.getString(i);
//					String subject = jobScheduler.getJSONObject(job).getString("subject");
//					String bodyTemplate = jobScheduler.getJSONObject(job).getString("body");
//					String url = applicationurl + "rpc/getremainderdata?datas=" + job;
//					JSONArray json = dataTrans.transmitDataspgrest(url,schema);
//
//					for (int list = 0; list < json.length(); list++) {
//						JSONObject jsondata = new JSONObject(json.get(list).toString());
//						String companyName = jsondata.getString("primarydata").split("\\+")[0];
//						String docsname = jsondata.getString("docsname");
//						String expiryDate = jsondata.getString("primarydata").split("\\+")[1];
//						String body = bodyTemplate.replace("{companyName}", companyName).replace("{docsname}", docsname)
//								.replace("{expiryDate}", expiryDate);
//
//						if (job.equals("fleet")) {
//							String fleetID = jsondata.getString("primarydata").split("\\+")[2];
//							body = body.replace("{fleetID}", fleetID);
//						}
//						resultOfMail = amazonSMTPMail.sendEmail(smtpMail.getString("amazonverifiedfromemail"),
//								jsondata.getString("email"), subject, body, smtpMail.getString("amazonsmtpusername"),
//								smtpMail.getString("amazonsmtppassword"), smtpMail.getString("amazonhostaddress"),
//								smtpMail.getString("amazonport"));
//					}
//				} catch (Exception e) {
//					LOGGER.error(Thread.currentThread().getStackTrace()[0].getMethodName(), e);
//				}
//			}
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
			String url = applicationurl + "rpc/list_properties_with_expiring_rentals?datas=e.enddate='"
					+ TimeZoneServices.getDate(new Date()) + "'";
			JSONArray json = dataTrans.transmitDataspgrest(url,schema);
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
						dataTrans.transmitDataspgrestpost(url, setvalue.toString(), false,schema);
						res = ResponcesHandling.sendPushNotification(setvalue, "activitylog", "tenant", new JSONObject(),schema);
					}
					MessageServices.MsegatsmsService(datavalue.getString("mobile"),
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
