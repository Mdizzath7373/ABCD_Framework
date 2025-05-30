package com.eit.abcdframework.config;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import jakarta.annotation.PostConstruct;

@Component
@Qualifier(value = "configurationFile")
public class ConfigurationFile {
	Logger log = LoggerFactory.getLogger(ConfigurationFile.class);
	private static Config config;
	private static Config googleAuth;

	@PostConstruct
	public void getConfigJsonFile() {
		String data = "";
		try {
			ClassPathResource classpathresource = new ClassPathResource("config.json");
			try (InputStream inputStream = classpathresource.getInputStream()) {
				data = new String(inputStream.readAllBytes());
			}
			if (data != null && !data.equalsIgnoreCase("")) {
				log.error(data);
				config = ConfigFactory.parseString(data);
			}
		} catch (Exception e) {
			log.error("Exception at : {} {}", Thread.currentThread().getName(), "Config Errors");
		}
	}

	@PostConstruct
	public void getGoogleAuthJson() {
		String data = "";
		try {
			ClassPathResource classpathresource = new ClassPathResource("googleAuth.json");
			try (InputStream inputStream = classpathresource.getInputStream()) {
				data = new String(inputStream.readAllBytes());
			}
			if (data != null && !data.equalsIgnoreCase("")) {
				log.error("Load a Google Auth Of Notification");
				googleAuth = ConfigFactory.parseString(data);
			}
		} catch (Exception e) {
			log.error("Exception at : {} {}", Thread.currentThread().getName(), "Config Errors");
		}
	}

	public static String getStringConfig(String path) {
		try {
			if (hasConfigpath(path)) {
				return config.getString(path.toString());

			} else {
				return null;
			}
		} catch (Exception e) {
			return null;
		}
	}

	public static List<String> getListStringConfig(String path) {
		try {
			if (hasConfigpath(path)) {
				config.getStringList(path.toString());
				return config.getStringList(path.toString());

			} else {
				return null;
			}
		} catch (Exception e) {
			return null;
		}
	}

	public static String getPushNotificationAuthJson() {
		if (googleAuth != null) {
			try {
				Config pushNotificationAuthConfig = googleAuth.getConfig("pushNotificationAuth");
				ObjectMapper objectMapper = new ObjectMapper();
				Map<String, Object> map = pushNotificationAuthConfig.root().unwrapped();
				return objectMapper.writeValueAsString(map);
			} catch (Exception e) {
				return null;
			}
		}
		return null;
	}

	public static List getListConfig(String path) {
		try {
			if (hasConfigpath(path)) {
				return config.getList(path.toString());

			} else {
				return null;
			}
		} catch (Exception e) {
			return null;
		}
	}

	public static Boolean getBooleanConfig(String path) {
		try {
			if (hasConfigpath(path)) {
				return config.getBoolean(path.toString());

			} else {
				return false;
			}
		} catch (Exception e) {
			return false;
		}
	}

	public static String[] getRequestMatcherConfig(String path) {
		try {
			List<String> obj = getListStringConfig(path);
			return obj.stream().toArray(String[]::new);

		} catch (Exception e) {
			return null;
		}
	}

	public static Boolean hasConfigpath(Object path) {
		return config.hasPath(path.toString());
	}

	public static Boolean hasgooleAuthpath(Object path) {
		return googleAuth.hasPath(path.toString());
	}

	public static Long getLong(String path) {
		try {
			if (hasConfigpath(path)) {
				return config.getLong(path.toString());

			} else {
				return null;
			}
		} catch (Exception e) {
			return null;
		}
	}

}
