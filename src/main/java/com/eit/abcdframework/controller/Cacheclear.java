package com.eit.abcdframework.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.eit.abcdframework.http.caller.Httpclientcaller;
import com.eit.abcdframework.serverbo.DisplaySingleton;

@RestController
@RequestMapping("/cacheclear")
@CrossOrigin("*")
public class Cacheclear {
	@Autowired
	DisplaySingleton displaySingleton;
	
	@Autowired
	Httpclientcaller httpclientcaller;

	@GetMapping("/cron")
	public void cronRefresh() {
		displaySingleton.scheduleJobs();
	}
	
	@GetMapping("/configs")
	public void configclear() {
		displaySingleton.configsObj();
	}

	@GetMapping("/applictionsetting")
	public void applictionsetting() {
		displaySingleton.applictionsettingObj();
	}

	@GetMapping("/emailconfig")
	public void emailconfig() {
		displaySingleton.emailConfigObj();
	}

	@GetMapping("/test")
	public void test() {

	}

//	@GetMapping("/companydeatils")
//	public String companydetails() throws ParseException, IOException {
//		String date = null;
//		JSONObject datavalues=null;
//		System.err.println(TimeZoneServices.getDateInTimeZoneforSKT(new Date(), "Asia/Riyadh"));
//		System.err.println(TimeZoneServices.getDateInTimeZoneforSKT("Asia/Riyadh"));
//		try {
//			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
//			date =  sdf.format(TimeZoneServices.getDateInTimeZoneforSKT(new Date(), "Asia/Riyadh"));
//			JSONObject json=new JSONObject();
//			json.put("dateofvalue",TimeZoneServices.getDateInTimeZoneforSKT("Asia/Riyadh") );
//			json.put("dateofvalue2", date);
//			httpclientcaller.transmitDataspgrestpost("http://ge-fleetonqa.thegoldenelement.com:3000/test",json.toString() );
//			datavalues=new JSONObject().put("datavlaues",httpclientcaller.transmitDataspgrest("http://ge-fleetonqa.thegoldenelement.com:3000/test"));
// 			System.err.println(datavalues);
//		} catch (Exception e) {
//          System.err.println(e);
//		}
//
//		return datavalues.toString();
//	}
}
