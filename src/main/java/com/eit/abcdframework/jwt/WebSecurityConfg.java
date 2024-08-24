package com.eit.abcdframework.jwt;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.eit.abcdframework.config.ConfigurationFile;

@Configuration
@EnableWebSecurity
public class WebSecurityConfg {

	@Autowired
	JwtRequestFilter jwtRequestFilter;

	@Autowired
	JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
	@Autowired
	UserDetailsService userDetailsService;

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	public AuthenticationManager authenticationManagerBean(AuthenticationConfiguration auth) throws Exception {
		return auth.getAuthenticationManager();
	}

	// New Method for WebSecurity
	@Bean
	protected SecurityFilterChain filterChain(HttpSecurity httpSecurity) throws Exception {
		if (ConfigurationFile.getBooleanConfig("jwt.enabled") != null
				&& ConfigurationFile.getBooleanConfig("jwt.enabled")) {
			if (ConfigurationFile.getBooleanConfig("jwt.cors") != null
					&& ConfigurationFile.getBooleanConfig("jwt.cors")) {
				httpSecurity.cors().and().csrf(csrf -> csrf.disable());
			}
			if (ConfigurationFile.getRequestMatcherConfig("jwt.permitall") != null
					&& ConfigurationFile.getRequestMatcherConfig("jwt.permitall").length > 0) {
				httpSecurity.authorizeHttpRequests(
						auth -> auth.requestMatchers(ConfigurationFile.getRequestMatcherConfig("jwt.permitall"))
								.permitAll().anyRequest().authenticated());
			} else {
				httpSecurity.authorizeHttpRequests(auth -> auth.anyRequest().authenticated());
			}
			httpSecurity.exceptionHandling(excep -> excep.authenticationEntryPoint(jwtAuthenticationEntryPoint))
					.sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
			httpSecurity.addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);
		} else {
			httpSecurity.cors().and().csrf(csrf -> csrf.disable());

			httpSecurity.authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
					.exceptionHandling(excep -> excep.authenticationEntryPoint(jwtAuthenticationEntryPoint))
					.sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
			httpSecurity.addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);
		}
		return httpSecurity.build();
	}
}