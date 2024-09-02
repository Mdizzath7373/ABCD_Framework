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
import com.eit.abcdframework.service.FormdataService;

@RestController
@RequestMapping("/Dcservice")
@CrossOrigin("*")
public class FormdataController {

	@Autowired
	FormdataService formdataService;

	@Autowired
	AmazonS3 amazonS3;

	@GetMapping("/form")
	public String transmittingDataget(@RequestParam String name, @RequestParam String primary,
			@RequestParam String where) {
		return formdataService.transmittingDataget(name, primary, where);
	}

	@PostMapping("/form")
	public String transmittingDatapost(@RequestBody String data) {
		return formdataService.transmittingDatapost(data);
	}

	@PutMapping("/form")
	public String transmittingDataput(@RequestBody String data) {
		return formdataService.transmittingDataput(data);
	}

	@DeleteMapping("/form")
	public String transmittingDataDel(@RequestParam String name, @RequestParam String primary,
			@RequestParam String where, boolean isdeleteall) {
		return formdataService.transmittingDataDel(name, primary, where, isdeleteall);
	}

	@GetMapping("/download")
	public ResponseEntity<InputStreamResource> downloadFileFromS3(String url) throws IOException {
		url = url.split("onboard/")[1];
		S3Object s3Object = amazonS3.getObject("goldenelement", "onboard/"+url);
		

		InputStreamResource resource = new InputStreamResource(s3Object.getObjectContent());

		HttpHeaders headers = new HttpHeaders();
		headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + "onboard/"+url);

		return ResponseEntity.ok().headers(headers).contentType(MediaType.APPLICATION_OCTET_STREAM).body(resource);
	}
}
