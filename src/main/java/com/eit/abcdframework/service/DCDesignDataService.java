package com.eit.abcdframework.service;

import java.util.List;

import org.json.JSONObject;
import org.springframework.web.multipart.MultipartFile;

import com.eit.abcdframework.dto.CommonUtilDto;

public interface DCDesignDataService {

	CommonUtilDto getDCDesignData(String data);

//	String fileupload(List<MultipartFile> files, String data);

	String getwidgetsdata(String data);


	String getDCDesignChart(String data);
	
//	String fileuploadwithprogress(MultipartFile files, String data);

	void getProgress(String data);

	
	String SplitterPDFChanges(JSONObject jsonObject1);

//	String mergeToPDF(MultipartFile files, String data);

	String fileUpload(List<MultipartFile> files, String data,String transmitMethod);

	String uploadImageProgress(List<MultipartFile> files,String data);

	String multiGridDesignData(String data);

}
