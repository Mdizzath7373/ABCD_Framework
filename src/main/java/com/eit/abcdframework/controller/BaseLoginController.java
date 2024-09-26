package com.eit.abcdframework.controller;

import java.util.Base64;

import org.apache.commons.httpclient.HttpStatus;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.GetMethod;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.eit.abcdframework.dto.JwtRequest;
import com.eit.abcdframework.http.caller.Httpclientcaller;
import com.eit.abcdframework.serverbo.CommonServices;
import com.eit.abcdframework.serverbo.DisplaySingleton;
import com.eit.abcdframework.serverbo.JwtServices;
import com.eit.abcdframework.service.BaseLoginService;
import com.eit.abcdframework.util.AmazonSMTPMail;
import com.eit.abcdframework.util.Location;
import com.eit.abcdframework.util.MessageServices;

@RestController
@RequestMapping(value = "/basiclogin", produces = MediaType.APPLICATION_JSON_VALUE)
@CrossOrigin("*")
public class BaseLoginController {
	private static final Logger LOGGER = LoggerFactory.getLogger(BaseLoginController.class);

	@Autowired
	DisplaySingleton displaySingleton;
	@Autowired
	Httpclientcaller dataTransmits;

	@Autowired
	JwtServices jwtServices;

	@Autowired
	CommonServices commonServices;

	@Autowired
	BaseLoginService loginservice;

	@Value("${applicationurl}")
	private String pgresturl;

	@Autowired
	AmazonSMTPMail amazonSMTPMail;

	@Autowired
	MessageServices messageServices;

	@Autowired
	private PasswordEncoder encoder;

	@Autowired
	Location location;
	
	@Value("${schema}")
	private String schema;

//	
	@PostMapping(value = "/company", produces = { "application/json" })
	public String getApplicationLoginData(@RequestBody String data) {
		JSONObject returnMessage = new JSONObject();
		JSONArray user = null;
		String token = "";
		try {
			JSONObject commonJson = new JSONObject(data);
			String name = commonJson.getString("username");
			String pass = commonJson.getString("password");
			String pgrest = pgresturl + "comuser";
			user = dataTransmits.transmitDataspgrest(pgrest,schema);
			if (user.isEmpty()) {
				return returnMessage.put("error", "No user was found,Please Enter the vaild credential").toString();
			}

			String Encodepassword = new JSONObject(user.get(0).toString()).getString("userpasswd");
			String isCheckToken = commonJson.has("isMoblie") ? commonJson.getString("isMoblie") : "web";
			token = jwtServices.verfieByUserAndGenarteToken(new JwtRequest(name, Encodepassword, pass), isCheckToken)
					.getToken();
		} catch (Exception e) {
			LOGGER.error("LoginController -->{}", e);
			return returnMessage.put("error", e.getMessage()).toString();
		}
		return loginservice.authenticateUser(user, token);

	}

//	@GetMapping(value = "/sendOTP", produces = { "application/json" })
//	public String sendOTP(String username, String OTP) {
//		JSONObject returnMessage = new JSONObject();
//		String companyname = "";
//		try {
//			String email = "";
////			String url = (pgresturl + "rpc/checkuser?datas=" + username + "&name=logincheck").replace(" ", "%20");
//			String url = (pgresturl + "comuser?emailaddress=eq." + username);
//			JSONArray data = dataTransmits.transmitDataspgrest(url);
//			if (data.isEmpty())
//				return returnMessage.put("error", "Company Not Found Please Check").toString();
//			else {
//				if (new JSONObject(data.get(0).toString()).getString("rolename").equalsIgnoreCase("Airport Officer")) {
//					email = new JSONObject(data.get(0).toString()).getString("key");
//				} else {
//					email = username;
//				}
//				companyname = new JSONObject(data.get(0).toString()).getString("companyname");
//			}
//			long OTPs = (long) Math.floor(100000 + Math.random() * 900000);
//
//			String subject = "Your " + companyname + " Password Recovery";
//			String content = " Hi <span style=\"color: red\">" + username + ",</span><br>"
//					+ " We received a request to reset the password on your <span style=\"color: red\"> " + companyname
//					+ "</span> Company." + "<br><h1><#>" + OTPs
//					+ "</h1> Enter this code to complete the reset.<br>Thanks for helping us keep your account secure.";
//
//			JSONObject smtpMail = new JSONObject(
//					DisplaySingleton.memoryApplicationSetting.get("smptAmazonMail").toString());
//
//			String status = amazonSMTPMail.sendEmail(smtpMail.getString("amazonverifiedfromemail"), email, subject,
//					content, smtpMail.getString("amazonsmtpusername"), smtpMail.getString("amazonsmtppassword"),
//					smtpMail.getString("amazonhostaddress"), smtpMail.getString("amazonport"));
//
//		} catch (Exception e) {
//			returnMessage.put("error", "Failed to getJson, Please Wait ");
//			LOGGER.error("Exception at " + Thread.currentThread().getStackTrace()[1].getMethodName(), e);
//		}
//
//		return returnMessage.toString();
//	}

	@GetMapping(value = "/forgotpassword", produces = { "application/json" })
	public String forgotpassword(String pass, String id, String otp) {
		JSONObject returnMessage = new JSONObject();
		String response = "";
		String res = "";
		try {
			String responseOfVerification = commonServices.VerificationOTP(otp, id);
			if (responseOfVerification.equalsIgnoreCase("verified")) {
				JSONObject forgotJson = new JSONObject(
						DisplaySingleton.memoryApplicationSetting.get("forgotpassConfig").toString());
				String url = (pgresturl + "/" + forgotJson.getString("tablename") + "?"
						+ forgotJson.getString("Findcolumn") + "=eq." + id).replace(" ", "%20");
				JSONObject jsonbody = new JSONObject(dataTransmits.transmitDataspgrest(url,schema).get(0).toString());
				byte[] decodedBytes = Base64.getDecoder().decode(pass);
				String decodedString = new String(decodedBytes);
				jsonbody.put(forgotJson.getString("columnname"),
						forgotJson.getBoolean("encode") ? encoder.encode(decodedString) : decodedString);
				url = (pgresturl + "/" + forgotJson.getString("tablename") + "?" + forgotJson.getString("primarycolumn")
						+ "=eq." + jsonbody.get(forgotJson.getString("primarycolumn"))).replace(" ", "%20");
				response = dataTransmits.transmitDataspgrestput(url, jsonbody.toString(), false,schema);
				if (Integer.parseInt(response) >= 200 && Integer.parseInt(response) <= 226) {
					returnMessage.put("reflex", "Change Successfully");
				} else {
					res = HttpStatus.getStatusText(Integer.parseInt(response));
					returnMessage.put("error", res);
				}
			} else {
				returnMessage.put("error", responseOfVerification);
			}
		} catch (Exception e) {
			returnMessage.put("error", "Failed");
			LOGGER.error("Exception at " + Thread.currentThread().getStackTrace()[1].getMethodName(), e);
		}
		return returnMessage.toString();
	}

	@PostMapping("/sendNotificationid")
	public String saveNotificationid(@RequestBody String data) {
		String res = "";
		try {
			JSONObject notification = new JSONObject(
					DisplaySingleton.memoryApplicationSetting.get("notificationConfig").toString());
			JSONObject commonJson = new JSONObject(data);
			boolean checkuser = commonJson.has("findbyuser") ? commonJson.getBoolean("findbyuser") : true;
			String id = commonJson.has("notificationid") ? commonJson.getString("notificationid") : "";
			String notifitoke = commonJson.has("notificationtoken") ? commonJson.getString("notificationtoken") : "";
			JSONObject user = null;
			JSONObject whereFormationJson = null;
			if (checkuser) {
				whereFormationJson = notification.getJSONObject("TokenSetByUser").getJSONObject("FindUser");
				String where = commonServices.whereFormation(commonJson, whereFormationJson);
				String pgrest = pgresturl + whereFormationJson.getString("tablename") + where;
				user = new JSONObject(dataTransmits.transmitDataspgrest(pgrest,schema).get(0).toString());
				System.err.println(user);
			}
			res = loginservice.pushNotificationUpdate(commonJson.getBoolean("tokenOn"), user, id, notifitoke,
					whereFormationJson);
		} catch (Exception e) {
			res = new JSONObject().put("error", "Failed").toString();
			LOGGER.error("Exception at " + Thread.currentThread().getStackTrace()[1].getMethodName(), e);
		}
		return res;
	}

	@PostMapping("/logoff")
	public String userLogoff(@RequestBody String data) {
		String res = "";
		try {
			JSONObject commonJson = new JSONObject(data);
			String id = commonJson.getString("id");
			res = commonServices.userstatusupdate("logout", new JSONObject(), id);
			if (new JSONObject(res.toString()).has("reflex")) {
				return res;
			}
		} catch (Exception e) {
			res = new JSONObject().put("error", "Failed").toString();
			LOGGER.error("Exception at " + Thread.currentThread().getStackTrace()[1].getMethodName(), e);
		}
		return res;

	}

	@GetMapping("/userActivation")
	public String userActivation(@RequestParam String key) {
		return commonServices.userActivation(key);
	}

	@GetMapping("/Verification")
	public String verification(@RequestParam String name, @RequestParam String value,@RequestParam String lang) {
		return loginservice.verification(name, value,lang);

	}

//	@GetMapping("/test")
//	public String test() throws IOException {
//		JSONArray arr = dataTransmits.transmitDataspgrest("http://ge-fleetonqa.thegoldenelement.com:3000/configs");
//		arr.forEach((arrs) -> ({
//			JSONObject obj = new JSONObject(arrs.toString());
//			LOGGER.warn(obj.get("id")+"ids");
//			JSONObject discfg = new JSONObject(obj.getString("discfg"));
////			discfg.remove("Airport Security");
//			
//			if (discfg.has("jqxdetails")) {
//				JSONObject jqxdetails = new JSONObject(discfg.getJSONObject("jqxdetails").toString());
//				if (jqxdetails.has("Reviewer")) {
//					jqxdetails.remove("Reviewer");
//				}
//				if (jqxdetails.has("showgridbyrole")) {
//					JSONObject showbyrole = new JSONObject(jqxdetails.getJSONObject("showgridbyrole").toString());
//					if (showbyrole.has("Reviewer")) {
//						showbyrole.remove("Reviewer");
//					}
//					if (showbyrole.has("Airport Admin") && !showbyrole.getJSONArray("Airport Admin").isEmpty()) {
//						showbyrole.put("Airport Officer", showbyrole.getJSONArray("Airport Admin"));
//					}
//					if (jqxdetails.has("Airport Admin") && !jqxdetails.getJSONArray("Airport Admin").isEmpty()) {
//						jqxdetails.put("Airport Officer", jqxdetails.getJSONArray("Airport Admin"));
//					}
//					jqxdetails.put("showgridbyrole", showbyrole);
//					discfg.put("jqxdetails", jqxdetails);
//					obj.put("discfg", discfg.toString());
//					System.out.println(obj);
//					String res = dataTransmits.transmitDataspgrestput(
//							"http://ge-fleetonqa.thegoldenelement.com:3000/configs?id=eq." + obj.get("id"),
//							obj.toString());
//					System.out.println(res);
//				}
//			}
//		});
//		return "success";
//	}

	@PostMapping(value = "/sendOTP", produces = { "application/json" })
	public String sendOTP(@RequestBody String reqBody) {
		return messageServices.MsegatOTPService(reqBody);
	}

	@PostMapping(value = "/verifyOTP", produces = { "application/json" })
	public String verifyOTP(@RequestBody String reqBody) {
		return messageServices.MsegatverifyOTP(reqBody);
	}

	@GetMapping("/location")
	public String getlocationwithradius(@RequestParam double centerLat, @RequestParam double centerLon,
			@RequestParam double radiusInKm) {
		return location.filteredLocationswithradius(centerLat, centerLon, centerLon);

	}
	@GetMapping(value="/geocoding", produces = { "application/json" })
    public String getLocation(@RequestParam String lat,@RequestParam String lon) {
		JSONObject returnMessage=new JSONObject();
        String key = DisplaySingleton.memoryApplicationSetting.get("locationApikey").toString();
        String locationUrl = DisplaySingleton.memoryApplicationSetting.get("locationUrl").toString();

            HttpClient client = new HttpClient();
            HttpMethod method = new GetMethod(locationUrl);

            // Setting the query parameters
            NameValuePair nvp1 = new NameValuePair("key", key);
            NameValuePair nvp2 = new NameValuePair("lat", String.valueOf(lat));
            NameValuePair nvp3 = new NameValuePair("lon", String.valueOf(lon));
            NameValuePair nvp4 = new NameValuePair("format", "json");
            NameValuePair nvp5 = new NameValuePair("normalizeaddress", "1");

            method.setQueryString(new NameValuePair[]{nvp1, nvp2, nvp3, nvp4, nvp5});

            try {
                client.executeMethod(method);
                String response = method.getResponseBodyAsString();

                // Parse JSON response
                ObjectMapper mapper = new ObjectMapper();
                JsonNode rootNode = mapper.readTree(response);
                String address = rootNode.path("display_name").asText();
                returnMessage.put("datavalues", address);
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            } finally {
                method.releaseConnection();
            }
            return returnMessage.toString();
    }

}
