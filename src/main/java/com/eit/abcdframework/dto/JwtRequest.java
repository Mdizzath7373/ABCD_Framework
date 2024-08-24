package com.eit.abcdframework.dto;

import java.io.Serializable;

public class JwtRequest implements Serializable{
	private static final long serialVersionUID = 5926468583005150707L;
	
	private String getPaswword;
	public String getpass() {
		return getPaswword;
	}

	public void setpass(String getPaswword) {
		this.getPaswword = getPaswword;
	}

	private String username;
	private String password;
	

	//default constructor for JSON Parsing
	public JwtRequest()
	{
	}

	public JwtRequest(String username,String getPaswword, String password) {
		this.setUsername(username);
		this.setpass(getPaswword);
		this.setPassword(password);
	}

	public String getUsername() {
		return this.username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return this.password;
	}

	public void setPassword(String password) {
		this.password = password;
	}
}
