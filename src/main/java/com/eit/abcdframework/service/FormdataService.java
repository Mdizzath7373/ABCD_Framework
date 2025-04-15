package com.eit.abcdframework.service;

public interface FormdataService {

	String transmittingToMethod(String method,String data,String Which);
	
	String transmittingToMethod(String method, String name, String primary, String where, boolean isdeleteall);

	String transmittingToMethodBulk(String string, String data);


}
