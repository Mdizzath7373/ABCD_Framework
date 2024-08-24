package com.eit.abcdframework.controller;

import java.util.List;
import java.util.Map;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.eit.abcdframework.dto.CommonUtilDto;
import com.eit.abcdframework.serverbo.FileuploadServices;
import com.eit.abcdframework.service.DCDesignDataService;

@RestController
@RequestMapping("/DCDesignDataServlet")
@CrossOrigin("*")
public class DCDesignDataServlet {

	@Autowired
	DCDesignDataService dcDesignDataService;

	@Autowired
	FileuploadServices fileuploadServices;

	@Value("${applicationurl}")
	public String pgrest;

	private static final Logger LOGGER = LoggerFactory.getLogger("DCDesignDataServlet");

	@PostMapping(value = "/displaydata")
	public CommonUtilDto getDCDesignData(@RequestBody String data) {
		return dcDesignDataService.getDCDesignData(data);
	}

	@PostMapping(value = "/fileupload", produces = { "application/json" })
	public String fileupload(@RequestPart("files") List<MultipartFile> files, @RequestParam String data) {
		LOGGER.error("Entered into Fileupload");
		return dcDesignDataService.fileupload(files, data);
	}

	@PostMapping(value = "/getwidgets", produces = { "application/json" })
	public String getwidgets(@RequestBody String data) {
		return dcDesignDataService.getwidgetsdata(data);
	}

	@PostMapping(value = "/fileuploadforgeneratedpdf", produces = { "application/json" })
	public String fileuploadforgeneratedpdf(@RequestParam String file, @RequestParam String data) {
		JSONObject json = new JSONObject(file);
		String base64 = json.getString("file");
		return dcDesignDataService.fileuploadforgeneratedpdf(base64, data);
	}

	@PostMapping(value = "/singlefileupload", produces = { "application/json" })
	public String fileupload(@RequestPart("files") MultipartFile files) {
		LOGGER.error("Entered into Fileupload");
		return fileuploadServices.fileupload(files);
	}

	@PostMapping(value = "/getchartDesignData")
	public String getDCDesignChart(@RequestBody String data) {
		return dcDesignDataService.getDCDesignChart(data);
	}

	@PostMapping("/test")
	public  String test(@RequestPart("files") MultipartFile files) throws Exception {
//	public void test(@RequestPart("files") MultipartFile files) throws Exception {
		return fileuploadServices.convertPdfToMultipart(files);
//		fileuploadServices.dara(files);
		
	}
}
