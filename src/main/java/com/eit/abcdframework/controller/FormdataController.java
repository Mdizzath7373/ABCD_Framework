package com.eit.abcdframework.controller;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.S3Object;
import com.eit.abcdframework.config.ConfigurationFile;
import com.eit.abcdframework.http.caller.Httpclientcaller;
import com.eit.abcdframework.service.FormdataService;

@RestController
@RequestMapping("/Dcservice")
@CrossOrigin("*")
public class FormdataController {

	@Autowired
	FormdataService formdataService;

	@Autowired
	AmazonS3 amazonS3;

	@Autowired
	Httpclientcaller dHttpclientcaller;

	private String path = ConfigurationFile.getStringConfig("s3bucket.path");

	@GetMapping("/form")
	public String transmittingDataget(@RequestParam String name, @RequestParam String primary,
			@RequestParam String where) {
		return formdataService.transmittingToMethod("GET", name, primary, where, false);
	}

	@PostMapping("/form")
	public String transmittingDatapost(@RequestBody String data) {
		return formdataService.transmittingToMethod("POST", data, "");
	}

	@PutMapping("/form")
	public String transmittingDataput(@RequestBody String data) {
		return formdataService.transmittingToMethod("PUT", data, "");
	}

	@DeleteMapping("/form")
	public String transmittingDataDel(@RequestParam String name, @RequestParam String primary,
			@RequestParam String where, boolean isdeleteall) {
		return formdataService.transmittingToMethod("Delete", name, primary, where, isdeleteall);
	}

	@PostMapping("/bulkAdd")
	public String transmittingDatapostBulk(@RequestBody String data) {
		return formdataService.transmittingToMethodBulk("POST", data);
	}

	@PutMapping("/bulkEdit")
	public String transmittingDataputBulk(@RequestBody String data) {
		return formdataService.transmittingToMethodBulk("PUT", data);
	}

	@GetMapping("/download")
	public ResponseEntity<InputStreamResource> downloadFileFromS3(String url) throws IOException {
		url = url.split(path)[1];
		S3Object s3Object = amazonS3.getObject("goldenelement", path + url);

		InputStreamResource resource = new InputStreamResource(s3Object.getObjectContent());

		HttpHeaders headers = new HttpHeaders();
		headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + "onboard/" + url);

		return ResponseEntity.ok().headers(headers).contentType(MediaType.APPLICATION_OCTET_STREAM).body(resource);
	}

//	@GetMapping("/test")
//	public void test() throws IOException {
//		 JSONObject json = new JSONObject();
//	        json.put("curd", new String[]{"POST", "PUT", "GET"});
//	        json.put("UniqueColumn", "users_id");
//	        json.put("FindTable", "users");
//	        json.put("FetchColumn", "status");
//	        json.put("MatchBy", "Suspended");
//	        json.put("Message", "your account has been Suspended by REFA");
//		   Httpclientcaller ddaaa=new Httpclientcaller();
//		  ddaaa.transmitDataspgrest(pgrest+"configs").forEach(entry->{
////			 int id =new JSONObject(entry.toString()).getInt("id");
//            JSONObject data= new JSONObject(new JSONObject(entry.toString()).getString("datas"));
//            if(data.has("sms")) {
//            	System.err.println(new JSONObject(entry.toString()).getString("alias"));
//            }
//            
//            data.put("checkAPI", json);
////            System.err.println(data);
//		  });
//	}

//	@GetMapping("/test")
//	public void test() throws IOException {
//		AtomicInteger count = new AtomicInteger(0);
//		System.err.println("hi");
//
//		dHttpclientcaller.transmitDataspgrest(pgrest + "configs2", "onboard").forEach(entry -> {
//			JSONObject datvalue = new JSONObject(entry.toString());
//
//			JSONObject jsondata = new JSONObject(datvalue.get("discfg").toString());
//
//			datvalue.remove("id");
//			if (datvalue.getString("displaytypes").equalsIgnoreCase("grid")
//					|| datvalue.getString("displaytypes").equalsIgnoreCase("report")) {
//				JSONObject jq = jsondata.getJSONObject("jqxdetails");
//				if(jq.has("Actions")) {
//					jq.put("default", jq.getJSONArray("Actions"));
//					jq.remove("Actions");
//				}				
//
//				jsondata.put("jqdetails", jq);
//				jsondata.remove("jqxdetails");
//				jsondata.put("datavalues", new JSONArray());
//				datvalue.put("discfg", jsondata.toString());
//			}
//			System.err.println(count.getAndIncrement());
////			System.err.println(datvalue);
////
//			System.err.println(dHttpclientcaller.transmitDataspgrestpost(pgrest + "configs", datvalue.toString(), false,
//					"onboard"));
//
//		});
//	}

}
