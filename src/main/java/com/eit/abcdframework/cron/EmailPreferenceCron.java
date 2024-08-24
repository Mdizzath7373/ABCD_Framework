package com.eit.abcdframework.cron;

import org.json.JSONObject;
import java.util.Date;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.eit.abcdframework.serverbo.DisplaySingleton;
@Component
public class EmailPreferenceCron {

	@Autowired
	EmailPreferenceService emailPreferenceService;
	
	public static final Logger LOGGER = LoggerFactory.getLogger("EmailPreferenceCronScheduler");

		
//	@Scheduled(cron = "0 00 08 * * ?", zone = "Asia/Kolkata")
//	public void run() {
//		//	At 08:00:00am every day
//		try {
//			String emailPreference = emailPreferenceService.getCompanyEmailPre();
//			LOGGER.error("Email Preference Scheduler End's at " + new Date());
//		} catch (Exception e) {
//			LOGGER.error("Exception in Cumulative Summary Day Report Scheduler :: ", e);
//		}
//
//	}
}