package com.eit.abcdframework.serverbo;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.eit.abcdframework.config.ConfigurationFile;
import com.eit.abcdframework.dto.JwtRequest;
import com.eit.abcdframework.dto.JwtResponse;
import com.eit.abcdframework.http.caller.Httpclientcaller;
import com.eit.abcdframework.jwt.JwtTokenUtil;

@Service
public class JwtServices implements UserDetailsService {

	@Autowired
	private AuthenticationManager authenticationManager;

	@Autowired
	private JwtTokenUtil jwtTokenUtil;

	@Autowired
	private Httpclientcaller httpclientcaller;

	@Value("${applicationurl}")
	private String pgresturl;

	@Value("${schema}")
	private String schema;

	private String istokencheck;

	public void tokenCheck(String value) {
		istokencheck = value;
	}

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

		UserDetails userDetails = null;
		if (username.contains(",")) {
			return new org.springframework.security.core.userdetails.User(username.split(",")[0],
					username.split(",")[1], new ArrayList<>());
		} else {
			String name = "";
			String pass = "";
			String getValueOf = istokencheck.equalsIgnoreCase("mobile") ? "Mobile" : "Web";
//		System.out.println("jwt.FindUser"+getValueOf+".function");

			boolean function = ConfigurationFile.getBooleanConfig("jwt.FindUser" + getValueOf + ".function");
			JSONArray params = new JSONArray(
					ConfigurationFile.getRequestMatcherConfig("jwt.FindUser" + getValueOf + ".param"));
			JSONArray value = new JSONArray(
					ConfigurationFile.getRequestMatcherConfig("jwt.FindUser" + getValueOf + ".value"));
			String pgrest = pgresturl + ConfigurationFile.getStringConfig("jwt.FindUser" + getValueOf + ".tablename");
			if (Integer.parseInt((ConfigurationFile.getStringConfig("jwt.primaryValue"))) > 1)
				username = username.split("#")[0];
			if (!params.isEmpty()) {
				for (int i = 0; i < params.length(); i++) {

					if (function) {
						if (i == 0)
							pgrest = pgrest + "?" + params.get(i) + "=" + username;
						else
							pgrest = pgrest + "&" + params.get(i) + "=" + value.get(i);
					} else {
						if (i == 0)
							pgrest = pgrest + "?" + params.get(i) + "=eq." + username;
						else
							pgrest = pgrest + "&" + params.get(i) + "=eq." + value.get(i);
					}
				}
			}
			try {
				JSONArray datavalue = httpclientcaller.transmitDataspgrest(pgrest, schema);
				name = new JSONObject(datavalue.get(0).toString())
						.getString(ConfigurationFile.getStringConfig("jwt.FindUser" + getValueOf + ".name"));
				pass = new JSONObject(datavalue.get(0).toString())
						.getString(ConfigurationFile.getStringConfig("jwt.FindUser" + getValueOf + ".pass"));
			} catch (Exception e) {
				new UsernameNotFoundException(e.getMessage());
			}
			userDetails = new org.springframework.security.core.userdetails.User(name, pass, new ArrayList<>());
		}
		return userDetails;
	}

	public JwtResponse verfieByUserAndGenarteToken(JwtRequest authenticationRequest, String ismoblie) throws Exception {
		authenticate(authenticationRequest.getUsername() + "," + authenticationRequest.getpass(),
				authenticationRequest.getPassword());
		tokenCheck(ismoblie);
		final UserDetails userDetails = loadUserByUsername(
				authenticationRequest.getUsername() + "," + authenticationRequest.getpass());
		return new JwtResponse(jwtTokenUtil.generateToken(userDetails, ismoblie));
	}

	private void authenticate(String username, String password) throws Exception {
		try {
			authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(username, password));
		} catch (DisabledException e) {
			throw new Exception("USER_DISABLED", e);
		} catch (BadCredentialsException e) {
			throw new BadCredentialsException("INVALID_CREDENTIALS", e);
		}
	}

}
