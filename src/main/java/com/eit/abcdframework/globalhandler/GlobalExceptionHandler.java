package com.eit.abcdframework.globalhandler;

import org.json.JSONObject;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalExceptionHandler {
	public static final String MISSINGPRIMARYKEY = "primary key is missing";
	private static final String ERROR = "error";
	private static final String FAILURE = "Failure";
	private static final String UNKNOWN = "unknown exception";
	private static final String APIFAILURE = "concurrent api failure";
	
    public static String handlePgRestError(JSONObject errorJson) {
        String code = errorJson.optString("code", "XXXXX");
        String message = PostgreSQLErrorCode.getDescriptionByCode(code);
        return message;
    }
    
    @ModelAttribute("MISSINGPRIMARYKEY")
    public static String missingPrimaryKey() {
		return MISSINGPRIMARYKEY;
	}
    
    @ModelAttribute("ERROR")
    public static String getError() {
		return ERROR;
	}
    
    @ModelAttribute("FAILURE")
    public static String getFailure() {
		return FAILURE;
	}
    
    @ModelAttribute("UNKNOWN")
    public static String getUnknownException() {
		return UNKNOWN;
	}
    
    @ModelAttribute("APIFAILURE")
    public static String getConcurrentApiFailure() {
		return APIFAILURE;
	}
}
