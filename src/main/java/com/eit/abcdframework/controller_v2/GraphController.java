package com.eit.abcdframework.controller_v2;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.eit.abcdframework.service_v2.GraphService;

@RestController
@RequestMapping("/graph")
@CrossOrigin("*")
public class GraphController {
	
	@Autowired
	GraphService gs;
	
	
	@PostMapping("/getchart")
	public String getChart(@RequestBody String data) {
		return gs.getChart(data);
	}
	
	

}
