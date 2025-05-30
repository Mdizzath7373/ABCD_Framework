package com.eit.abcdframework.jwt;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import com.eit.abcdframework.config.ConfigurationFile;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

@Component
public class JwtTokenUtil {

//	public static final long JWT_TOKEN_VALIDITY = 3600000;
//	public static final long JWT_TOKEN_VALIDITY_MOBLIE = 60000;

//	private static final Logger LOGGER = LoggerFactory.getLogger(JwtTokenUtil.class);

	public String getUsernameFromToken(String token) {
		return getClaimFromToken(token, Claims::getSubject);
	}

	public String getIdFromToken(String token) {
		return getClaimFromToken(token, Claims::getId);
	}

	public Date getIssuedAtDateFromToken(String token) {
		return getClaimFromToken(token, Claims::getIssuedAt);
	}

	public Date getExpirationDateFromToken(String token) {
		return getClaimFromToken(token, Claims::getExpiration);
	}

	public <T> T getClaimFromToken(String token, Function<Claims, T> claimsResolver) {
		final Claims claims = getAllClaimsFromToken(token);
		return claimsResolver.apply(claims);
	}

	private Claims getAllClaimsFromToken(String token) {
		return Jwts.parser().setSigningKey(ConfigurationFile.getStringConfig("jwt.secret")).parseClaimsJws(token)
				.getBody();
	}

	private Boolean isTokenExpired(String token) {
		final Date expiration = getExpirationDateFromToken(token);
		return expiration.before(new Date());
	}

	private Boolean ignoreTokenExpiration(String token) {
		return false;
	}

	public String generateToken(UserDetails userDetails, String ismoblie) {
		Map<String, Object> claims = new HashMap<>();
		return doGenerateToken(claims, userDetails.getUsername(), ismoblie);
	}

	private String doGenerateToken(Map<String, Object> claims, String subject, String ismoblie) {
//JWT_TOKEN_VALIDITY*1000
		long currTimeMillis = System.currentTimeMillis();
		Date isuuedTime = new Date(currTimeMillis);
//		Date expiredTime = new Date(
//				currTimeMillis + (!ismoblie.equalsIgnoreCase("web") ? JWT_TOKEN_VALIDITY_MOBLIE : JWT_TOKEN_VALIDITY));
		if (!ismoblie.equalsIgnoreCase("web")
				&& ConfigurationFile.getStringConfig("jwt.JWT_TOKEN_VALIDITY_MOBLIE").equalsIgnoreCase("")) {
			return Jwts.builder().setSubject(subject).setId("Mobile").setIssuedAt(isuuedTime)
					.signWith(SignatureAlgorithm.HS512, ConfigurationFile.getStringConfig("jwt.secret")).compact();
		} else if (!ismoblie.equalsIgnoreCase("web")
				&& !ConfigurationFile.getStringConfig("jwt.JWT_TOKEN_VALIDITY_MOBLIE").equalsIgnoreCase("")) {
			long validTo = Long.parseLong(ConfigurationFile.getStringConfig("jwt.JWT_TOKEN_VALIDITY_MOBLIE"));
			Date expiredTime = new Date(currTimeMillis + validTo);
			System.out.println(" currTimeMillis :: " + currTimeMillis + ", isuuedTime :: " + isuuedTime
					+ ", expiredTime :: " + expiredTime);
			return Jwts.builder().setSubject(subject).setId("Mobile").setIssuedAt(isuuedTime).setExpiration(expiredTime)
					.signWith(SignatureAlgorithm.HS512, ConfigurationFile.getStringConfig("jwt.secret")).compact();
		} else if (ismoblie.equalsIgnoreCase("web")
				&& ConfigurationFile.getStringConfig("jwt.JWT_TOKEN_VALIDITY").equalsIgnoreCase("")) {
			return Jwts.builder().setSubject(subject).setId("Web").setIssuedAt(isuuedTime)
					.signWith(SignatureAlgorithm.HS512, ConfigurationFile.getStringConfig("jwt.secret")).compact();
		} else if (ismoblie.equalsIgnoreCase("web")
				&& !ConfigurationFile.getStringConfig("jwt.JWT_TOKEN_VALIDITY").equalsIgnoreCase("")) {
			long validTo = Long.parseLong(ConfigurationFile.getStringConfig("jwt.JWT_TOKEN_VALIDITY"));
			Date expiredTime = new Date(currTimeMillis + validTo);
			System.out.println(" currTimeMillis :: " + currTimeMillis + ", isuuedTime :: " + isuuedTime
					+ ", expiredTime :: " + expiredTime);
			return Jwts.builder().setSubject(subject).setId("Web").setIssuedAt(isuuedTime).setExpiration(expiredTime)
					.signWith(SignatureAlgorithm.HS512, ConfigurationFile.getStringConfig("jwt.secret")).compact();
		}
		return "";

	}

	public Boolean canTokenBeRefreshed(String token) {
		return (!isTokenExpired(token) || ignoreTokenExpiration(token));
	}

	public Boolean validateToken(String token, UserDetails userDetails) {
		final String username = URLDecoder
				.decode((Integer.parseInt((ConfigurationFile.getStringConfig("jwt.primaryValue"))) > 1
						? getUsernameFromToken(token).split("#")[0]
						: getUsernameFromToken(token)), StandardCharsets.UTF_8);
		if (!getIdFromToken(token).equalsIgnoreCase("web"))
			return (username.equals(userDetails.getUsername()));
		else
			return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
	}

	public String updateToken(String token, String ismoblie, String subject) {
		String updatedToken = "";
		try {
			long currTimeMillis = System.currentTimeMillis();
			Date isuuedTime = new Date(currTimeMillis);

			if (!ismoblie.equalsIgnoreCase("web")
					&& ConfigurationFile.getStringConfig("jwt.JWT_TOKEN_VALIDITY_MOBLIE").equalsIgnoreCase("")) {

				updatedToken = Jwts.builder().setSubject(subject).setId("Moblie").setIssuedAt(isuuedTime)
						.signWith(SignatureAlgorithm.HS512, ConfigurationFile.getStringConfig("jwt.secret")).compact();
			} else if (!ismoblie.equalsIgnoreCase("web")
					&& !ConfigurationFile.getStringConfig("jwt.JWT_TOKEN_VALIDITY_MOBLIE").equalsIgnoreCase("")) {
				long validTo = Long.parseLong(ConfigurationFile.getStringConfig("jwt.JWT_TOKEN_VALIDITY_MOBLIE"));
				Date expiredTime = new Date(currTimeMillis + validTo);
				updatedToken = Jwts.builder().setSubject(subject).setId("Moblie").setIssuedAt(isuuedTime)
						.setExpiration(expiredTime)
						.signWith(SignatureAlgorithm.HS512, ConfigurationFile.getStringConfig("jwt.secret")).compact();
			} else if (ismoblie.equalsIgnoreCase("web")
					&& ConfigurationFile.getStringConfig("jwt.JWT_TOKEN_VALIDITY").equalsIgnoreCase("")) {
				updatedToken = Jwts.builder().setSubject(subject).setId("Web").setIssuedAt(isuuedTime)
						.signWith(SignatureAlgorithm.HS512, ConfigurationFile.getStringConfig("jwt.secret")).compact();
			} else if (ismoblie.equalsIgnoreCase("web")
					&& !ConfigurationFile.getStringConfig("jwt.JWT_TOKEN_VALIDITY").equalsIgnoreCase("")) {
				long validTo = Long.parseLong(ConfigurationFile.getStringConfig("jwt.JWT_TOKEN_VALIDITY"));
				Date expiredTime = new Date(currTimeMillis + validTo);
				updatedToken = Jwts.builder().setSubject(subject).setId("Web").setIssuedAt(isuuedTime)
						.setExpiration(expiredTime)
						.signWith(SignatureAlgorithm.HS512, ConfigurationFile.getStringConfig("jwt.secret")).compact();
			}
			System.out.println(updatedToken);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return updatedToken;
	}
}
