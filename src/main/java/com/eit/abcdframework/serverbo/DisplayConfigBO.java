package com.eit.abcdframework.serverbo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.eit.abcdframework.dto.CommonUtilDto;

@Service
public class DisplayConfigBO {

	public static final Logger LOGGER = LoggerFactory.getLogger(DisplayConfigBO.class);

	@Autowired
	DisplayHandler displayHandler;

	public CommonUtilDto getDesignData(String alias, boolean function, String role) {
//		String htmlStr = null;
		CommonUtilDto commonUtilDtoValue = null;
		try {
//			DisplayHandler displayHandler = new DisplayHandler();
			commonUtilDtoValue = displayHandler.toExecutePgRest(alias, function, role);
			if (commonUtilDtoValue == null) {
				return null;
			}
		} catch (Exception e) {
			LOGGER.error("Exception in getDesignData", e);
		}
		return commonUtilDtoValue;
	}

}
