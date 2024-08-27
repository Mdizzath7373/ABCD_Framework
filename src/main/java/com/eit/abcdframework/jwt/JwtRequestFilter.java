package com.eit.abcdframework.jwt;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.eit.abcdframework.config.ConfigurationFile;
import com.eit.abcdframework.serverbo.JwtServices;

import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtRequestFilter extends OncePerRequestFilter {

	@Autowired
	private JwtTokenUtil jwtTokenUtil;

	@Autowired
	JwtServices jwtServices;
	private static Map<String, String> lastupdToken = new HashMap<>();
//	private static Map<String, String> lastupdToken_moblie = new HashMap<>();
	private static final Logger LOGGER = LoggerFactory.getLogger("JwtRequestFilter");

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {

		final String requestTokenHeader = request.getHeader("Authorization");

//		logger.error("Request URL ---------> :: "+request.getRequestURI());

		String username = null;
		String jwtToken = null;
		String ischecktoken = null;
		;
		if (requestTokenHeader != null && requestTokenHeader.startsWith("Bearer ")) {
			jwtToken = requestTokenHeader.substring(7);
			try {
				if (ConfigurationFile.getBooleanConfig("jwt.refreshtoken")
						&& jwtTokenUtil.getIdFromToken(jwtToken).equalsIgnoreCase("web")) {
					if (lastupdToken.isEmpty() || !lastupdToken.containsKey(jwtToken)) {
						ischecktoken = jwtTokenUtil.getIdFromToken(jwtToken);
						if (ischecktoken.equalsIgnoreCase("web")) {
							username = jwtTokenUtil.getUsernameFromToken(jwtToken);
							if (jwtTokenUtil.canTokenBeRefreshed(jwtToken)) {
								lastupdToken.put(jwtToken, jwtTokenUtil.updateToken(jwtToken, ischecktoken, username));
								jwtToken = lastupdToken.get(jwtToken);
							}
						}
					} else {
						ischecktoken = jwtTokenUtil.getIdFromToken(jwtToken);
						if (ischecktoken.equalsIgnoreCase("web")) {
							username = jwtTokenUtil.getUsernameFromToken(lastupdToken.get(jwtToken));
							if (jwtTokenUtil.canTokenBeRefreshed(lastupdToken.get(jwtToken))) {
								lastupdToken.put(jwtToken,
										jwtTokenUtil.updateToken(lastupdToken.get(jwtToken), "web", username));
								jwtToken = lastupdToken.get(jwtToken);
							} else {
								lastupdToken.remove(username + "" + jwtToken);
							}
						}
					}
				} else {
					username = jwtTokenUtil.getUsernameFromToken(jwtToken);
				}
			} catch (IllegalArgumentException e) {
				System.out.println("Unable to get JWT Token");
			} catch (ExpiredJwtException e) {
				System.out.println("JWT Token has expired");
			}
		} else {
			LOGGER.warn("JWT Token does not begin with Bearer String");
		}

		if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
			jwtServices.tokenCheck(jwtTokenUtil.getIdFromToken(jwtToken));
			UserDetails userDetails = jwtServices.loadUserByUsername(username);
			LOGGER.info(username + "--->" + userDetails);
			if (jwtTokenUtil.validateToken(jwtToken, userDetails)) {
				UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken = new UsernamePasswordAuthenticationToken(
						userDetails, null, userDetails.getAuthorities());
				usernamePasswordAuthenticationToken
						.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
				SecurityContextHolder.getContext().setAuthentication(usernamePasswordAuthenticationToken);
			}
		}

		LOGGER.warn("Enter into doFilter Method");
		chain.doFilter(request, response);
	}

}
