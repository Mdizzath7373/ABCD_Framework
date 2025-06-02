package com.eit.abcdframework.cron;

import org.json.JSONObject;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import com.eit.abcdframework.service.FormdataServiceImpl;



public class CronJobs implements Job{

	@Autowired
	private FormdataServiceImpl formService;
	
	
	private static final Logger LOGGER = LoggerFactory.getLogger("CronJobs");
	
	
	@Override
	public void execute(JobExecutionContext context) throws JobExecutionException {
		try {
			
			LOGGER.info("CRON Executing...............");
			
		String aliasName = context.getJobDetail().getJobDataMap().getString("aliasName");
		//String result = "";
		
		//LOGGER.info("is Enabled : "+context.getJobDetail().getJobDataMap().getBoolean("isEnabled"));
		JSONObject json = new JSONObject().put("header", new JSONObject().put("name", aliasName)).put("body", new JSONObject());
		//LOGGER.info("json: "+ json.toString());
		
		if(context.getJobDetail().getJobDataMap().getBoolean("isEnabled"))
		formService.transmittingToMethod("POST",json.toString(),"","");
		
		//LOGGER.info("result : "+result);
		
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		
	}

}