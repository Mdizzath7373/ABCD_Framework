package com.eit.abcdframework.cronscheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.eit.abcdframework.cronservices.CronServices;

@Component
public class CronSchedularController {

	public static final Logger LOGGER = LoggerFactory.getLogger("CronSchedularController");
	private CronServices cronServices;

	@Autowired
	public void setProductService(@Qualifier("cronservice") CronServices service) {
		cronServices = service;
	}

//	@Scheduled(cron = "0 00 09 * * ?", zone = "Asia/Kolkata")
//	public void run() {
//		// At 09:00:00am every day
//		try {
//			String corn = cronServices.remainderThroughEmail("company");
//			LOGGER.error("Email Preference Scheduler End's at " + new Date());
//		} catch (Exception e) {
//			LOGGER.error("Exception in Cumulative Summary Day Report Scheduler :: ", e);
//		}
//		try {
//			String corn = cronServices.remainderThroughEmail("milestone");
//			LOGGER.error("Milestone reminder Scheduler End's at " + new Date());
//		} catch (Exception e) {
//			LOGGER.error("Exception in Milestone reminder Scheduler :: ", e);
//		}
//	}
//	@Scheduled(cron = "0 00 10 * * ?", zone = "Asia/Kolkata")
//	public void run1() {
//		//	At 10:00:00am every day
//		try {
//			String  corn=cronServices.remainderThroughEmail("fleet"); 
//			LOGGER.error("Email Preference Scheduler End's at " + new Date());
//		} catch (Exception e) {
//			LOGGER.error("Exception in Cumulative Summary Day Report Scheduler :: ", e);
//		}
//	}

//	@Scheduled(cron = "0 39 18 * * ?", zone = "Asia/Kolkata")
//	public void run2() {
//		//	At 10:00:00am every day
//		try {
//			String  corn=cronServices.triggerCorn(); 
//			LOGGER.error(" Scheduler End's at " + new Date());
//		} catch (Exception e) {
//			LOGGER.error("Exception in Cumulative Summary Day Report Scheduler :: ", e);
//		}
//	}

}
