package com.eit.abcdframework.service;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.eit.abcdframework.dto.CommonUtilDto;

public interface DCDesignDataService {

	CommonUtilDto getDCDesignData(String data);

	String fileupload(List<MultipartFile> files, String data);

	String getwidgetsdata(String data);

	String fileuploadforgeneratedpdf(String base64, String data);

	String getDCDesignChart(String data);
	
	String fileuploadwithprogress(MultipartFile files, String data);

	void getProgress(String data);

	String mergeToPDF(String data);

}
