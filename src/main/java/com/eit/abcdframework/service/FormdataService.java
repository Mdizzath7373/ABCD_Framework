package com.eit.abcdframework.service;

public interface FormdataService {

	String transmittingDatapost( String data);

	String transmittingDataget( String data, String primary, String where);

	String transmittingDataput( String data);

	String transmittingDataDel( String name, String primary, String where,boolean isdeleteall);


}
