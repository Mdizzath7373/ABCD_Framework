package com.eit.abcdframework.globalhandler;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalAttributeHandler {

	private static final String KEY = "primarykey";
	private static final String primaryColumnKey = "columnname";
	private static final String REFLEX = "reflex";
	private static final String SUCCESS = "Success";
	private static final String ERROR = "error";
	private static final String FAILURE = "Failure";
	private static final String DATAVALUE = "datavalue";

	// Define characters allowed in the verification code
	private static final String ALLOWED_CHARACTERS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";

	// Define the maximum length of the verification code
	private static final int MAX_CODE_LENGTH = 6;

	private static final String ALGORITHM = "AES/CBC/PKCS5Padding"; // AES with CBC and PKCS5 padding
	private static final String SECRET_KEY = "ABCDFRAM09876543"; // 16-byte key for AES
	private static final String IV = "ABCDFRAMIV098765"; // 16-byte IV for AES
	
	@Value("${applicationurl}")
	private static String pgrest;
	
	@Value("${schema}")
	private static String schema;
	
	@ModelAttribute("schema")
	public static String getSchema() {
		return schema;
	}
	
	
	@ModelAttribute("pgresturl")
	public static String getPgrest() {
		return pgrest;
	}
	@ModelAttribute("ALLOWED_CHARACTERS")
	public static String getAllowedCharacters() {
		return ALLOWED_CHARACTERS;
	}
	@ModelAttribute("MAX_CODE_LENGTH")
	public static int getMaxCodeLength() {
		return MAX_CODE_LENGTH;
	}
	@ModelAttribute("ALGORITHM")
	public static String getAlgorithm() {
		return ALGORITHM;
	}
	@ModelAttribute("SECRET_KEY")
	public static String getSecretKey() {
		return SECRET_KEY;
	}
	@ModelAttribute("IV")
	public static String getIv() {
		return IV;
	}

	@ModelAttribute("KEY")
	public static String getKey() {
		return KEY;
	}

	@ModelAttribute("primaryColumnKey")
	public static String getPrimarycolumnkey() {
		return primaryColumnKey;
	}

	@ModelAttribute("REFLEX")
	public static String getReflex() {
		return REFLEX;
	}

	@ModelAttribute("SUCCESS")
	public static String getSuccess() {
		return SUCCESS;
	}

	@ModelAttribute("ERROR")
	public static String getError() {
		return ERROR;
	}

	@ModelAttribute("FAILURE")
	public static String getFailure() {
		return FAILURE;
	}

	@ModelAttribute("DATAVALUE")
	public static String getDatavalue() {
		return DATAVALUE;
	}

}
