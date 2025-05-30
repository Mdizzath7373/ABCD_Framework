package com.eit.abcdframework.controller;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.eit.abcdframework.serverbo.CommonServices;
import com.eit.abcdframework.serverbo.WorkFlowEngine;

@RestController
@RequestMapping(value = "/WorkFlowEnginee", produces = MediaType.APPLICATION_JSON_VALUE)
@CrossOrigin("*")
public class WorkFlowEngineeController {

	@Autowired
	WorkFlowEngine workFlowEngineeService;

	@PostMapping("/Registration")
	public String Registration(@RequestBody String body) throws JSONException, Exception {
		JSONObject json = new JSONObject(body);
		if (json.has("data"))
			json = new JSONObject(CommonServices.decrypt(json.getString("data")));
		List<MultipartFile> file = new ArrayList<>();
		return workFlowEngineeService.registration(json.get("body").toString(), json.get("header").toString(), file);
	}

	@PostMapping("/RegistrationWithFile")
	public String RegistrationWithFile(@RequestPart List<MultipartFile> files, @RequestParam String body)
			throws JSONException, Exception {
		JSONObject json = new JSONObject(body);
		if (json.has("data"))
			json = new JSONObject(CommonServices.decrypt(json.getString("data")));
		return workFlowEngineeService.registration(json.get("body").toString(), json.get("header").toString(), files);
	}

	@PostMapping("/registration")
	public String registration(@RequestBody String body) throws JSONException, Exception {
		JSONObject json = new JSONObject(body);
		if (json.has("data"))
			json = new JSONObject(CommonServices.decrypt(json.getString("data")));
		List<MultipartFile> file = new ArrayList<>();
		return workFlowEngineeService.registration(json.get("body").toString(), json.get("header").toString(), file);
	}

	@PostMapping("/registrationWithFile")
	public String registrationWithFile(@RequestPart List<MultipartFile> files, @RequestParam String body)
			throws JSONException, Exception {
		JSONObject json = new JSONObject(body);
		if (json.has("data"))
			json = new JSONObject(CommonServices.decrypt(json.getString("data")));
		return workFlowEngineeService.registration(json.get("body").toString(), json.get("header").toString(), files);
	}

}
