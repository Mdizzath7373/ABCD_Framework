package com.eit.abcdframework.service;

import org.springframework.web.multipart.MultipartFile;

public interface FormdataService {

	String transmittingToMethod(String method,String data,String Which,String preRes);
	
	String transmittingToMethod(String method, String name, String primary, String where, boolean isdeleteall, boolean isbulk);

	String transmittingToMethodBulk(String method, String data);
	
	String insertViaExcel(MultipartFile file,String name);
	
	String transmittingToMethodDisassociate(String data);


}
