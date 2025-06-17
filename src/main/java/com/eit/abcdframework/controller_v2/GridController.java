package com.eit.abcdframework.controller_v2;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.eit.abcdframework.service_v2.GridService;

@RestController
@RequestMapping("/grid_v2")
@CrossOrigin("*")
public class GridController {
	
	@Autowired
	GridService gridService;
	
	@PostMapping(value = "/griddata_v2")
	public String fetchGridJSON_v2(@RequestBody String datas) {
		return gridService.fetchGridJSON_v2(datas);
	}
	
}
