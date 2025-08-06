package com.eit.abcdframework.controller_v2;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.eit.abcdframework.service_v2.FormService;

@RestController
@RequestMapping("/formData")
@CrossOrigin("*")
public class FormDataController {
	@Autowired
	FormService formService;
	//hii working
	//Hello
	// Mahabupkhan
	@PostMapping("/form")
	public String dataPost(@RequestBody String data) {
		return formService.transmttingToMethod("POST",data);
	}
	
	@PutMapping("/form")
	public String dataPut(@RequestBody String data) {
		return formService.transmttingToMethod("PUT",data);
	}
	
	@DeleteMapping("/form")
	public String dataDel(@RequestParam String aliasName,@RequestParam String deleteBy,@RequestParam String deleteContent) {
		return formService.transmittingToMethodDel(aliasName,deleteBy,deleteContent);
	}
	
	@PostMapping("/execute")
	public String executeQuery(@RequestBody String data) {
		return formService.executeQuery(data);
	}
	
}

