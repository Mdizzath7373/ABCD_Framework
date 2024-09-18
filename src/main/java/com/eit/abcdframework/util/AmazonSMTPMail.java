package com.eit.abcdframework.util;

import java.io.File;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import com.eit.abcdframework.http.caller.Httpclientcaller;
import com.eit.abcdframework.serverbo.DisplaySingleton;

import jakarta.activation.DataHandler;
import jakarta.activation.DataSource;
import jakarta.activation.FileDataSource;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.util.ByteArrayDataSource;

@Component
public class AmazonSMTPMail {

	private static final Logger LOGGER = LoggerFactory.getLogger("AmazonSMTPMail");
	private String status = "";
	private Transport transport;
	private static final String MAILTRANSPORTPROTOCOL = "mail.transport.protocol";
	private static final String MAILSMTPPORT = "mail.smtp.port";
	private static final String MAILSMTPAUTH = "mail.smtp.auth";
	private static final String MAILSMTPSTARTTLSENABLE = "mail.smtp.starttls.enable";
	private static final String MAILSMTPSTARTTLSREQUIRED = "mail.smtp.starttls.required";
	private static final String MAILSMTPSSLENABLE = "mail.smtp.ssl.enable";
	private static final String FAILED = "Failed";

	@Autowired
	DisplaySingleton displaySingleton;

	@Value("${applicationurl}")
	private String pgrest;

	@Value("${FromNameOfMail}")
	private String fromOfMail;

	@Autowired
	Httpclientcaller dataTransmit;

	public String sendEmail(String from, String to, String subject, String body, String smtpUser, String smtpPass,
			String host, String port) throws MessagingException {
		LOGGER.error("AmazonSMTPMail:: Entered:: for {}", to);
		try {
			Properties props = System.getProperties();
			props.put(MAILTRANSPORTPROTOCOL, "smtps");
			props.put(MAILSMTPPORT, port);
			props.put(MAILSMTPAUTH, "true");
			props.put(MAILSMTPSTARTTLSENABLE, "true");
			props.put(MAILSMTPSTARTTLSREQUIRED, "true");
			props.put(MAILSMTPSSLENABLE, "true");

			Session session = Session.getDefaultInstance(props);
			MimeMessage msg = new MimeMessage(session);
			msg.setFrom(new InternetAddress(from, fromOfMail));
			msg.setRecipient(Message.RecipientType.TO, new InternetAddress(to));
			msg.setSubject(subject);
			msg.setContent(body, "text/html;charset=utf-8");

			transport = session.getTransport();
			transport.connect(host, smtpUser, smtpPass);
			transport.sendMessage(msg, msg.getAllRecipients());

			status = "Success";
		} catch (Exception e) {
			LOGGER.error("AmazonSMTPMail:: Entered:: Logger :: From = " + from + ", Port = " + port + ", host = " + host
					+ ", smtpUser = " + smtpUser + ", to = " + to + " ::", e);
			status = FAILED;
		} finally {
			transport.close();
		}
		return status;
	}

	public String sendEmailWithFile(String from, String to, String subject, String body, String smtpUser,
			String smtpPass, String host, String port, List<MultipartFile> files) throws MessagingException {
		LOGGER.error("AmazonSMTPMail:: Entered:: for {}", to);
		try {
			Properties props = System.getProperties();
			props.put(MAILTRANSPORTPROTOCOL, "smtps");
			props.put(MAILSMTPPORT, port);
			props.put(MAILSMTPAUTH, "true");
			props.put(MAILSMTPSTARTTLSENABLE, "true");
			props.put(MAILSMTPSTARTTLSREQUIRED, "true");
			props.put(MAILSMTPSSLENABLE, "true");

			Session session = Session.getDefaultInstance(props);
			MimeMessage msg = new MimeMessage(session);

//			MimeMultipart multipart = new MimeMultipart();
			msg.setFrom(new InternetAddress(from, fromOfMail));
			msg.setRecipient(Message.RecipientType.TO, new InternetAddress(to));
			msg.setSubject(subject);

			MimeMultipart topLevelMultipart = new MimeMultipart();

			MimeBodyPart textPart = new MimeBodyPart();
			textPart.setContent(body, "text/html;charset=utf-8");
			topLevelMultipart.addBodyPart(textPart);

			MimeMultipart attachmentMultipart = new MimeMultipart();
			if (files != null) {
				for (MultipartFile file : files) {
					MimeBodyPart attachmentPart = new MimeBodyPart();
					ByteArrayDataSource source = new ByteArrayDataSource(file.getInputStream(), file.getContentType());

					attachmentPart.setDataHandler(new DataHandler(source));
					attachmentPart.setFileName(file.getOriginalFilename());
					attachmentMultipart.addBodyPart(attachmentPart);
				}
			}

			MimeBodyPart attachmentMultipartPart = new MimeBodyPart();
			attachmentMultipartPart.setContent(attachmentMultipart);
			topLevelMultipart.addBodyPart(attachmentMultipartPart);

			msg.setContent(topLevelMultipart);

			transport = session.getTransport();
			transport.connect(host, smtpUser, smtpPass);
			transport.sendMessage(msg, msg.getAllRecipients());

			status = "Success";
		} catch (Exception e) {
			LOGGER.error("AmazonSMTPMail:: Entered:: Logger :: From = " + from + ", Port = " + port + ", host = " + host
					+ ", smtpUser = " + smtpUser + ", to = " + to + " ::", e);
			status = FAILED;
		} finally {
			transport.close();
		}
		return status;
	}

	public String mailSender2(JSONArray mail, String email, JSONObject getJson, JSONObject jsonBody,
			List<MultipartFile> files, String lang) {
		String resultOfMail = "";
		String body = "";
		JSONArray mailContent = null;
		try {
			JSONObject smtpMail = new JSONObject(
					DisplaySingleton.memoryApplicationSetting.get("smptAmazonMail").toString());
			for (int c = 0; c < mail.length(); c++) {
				String url = pgrest + "emailconfig?name=eq." + mail.get(c);
				mailContent = dataTransmit.transmitDataspgrest(url.replace(" ", "%20"));
				for (int i = 0; i < mailContent.length(); i++) {
					JSONObject jsondata = new JSONObject(mailContent.get(i).toString());
					if (getJson != null && getJson.has("Key")) {
						String key = generatekey();
						jsonBody.put("key", key);
						body = new JSONObject(jsondata.get("contenttype").toString()).getString(lang).replace("key",
								key);
					} else if (!jsondata.getBoolean("custommail")) {
						if (getJson.has(mail.get(c) + "replacementContent")) {
							if (new JSONObject(getJson.get(mail.get(c) + "replacementContent").toString())
									.has("replace")) {
								JSONArray replaceData = new JSONObject(
										getJson.get(mail.get(c) + "replacementContent").toString())
										.getJSONArray("replace");
								JSONArray column = new JSONObject(
										getJson.get(mail.get(c) + "replacementContent").toString())
										.getJSONArray("column");
								for (int r = 0; r < replaceData.length(); r++) {
									if (r == 0) {
//										body = new JSONObject(jsondata.getString("contenttype").replace(replaceData.getString(r),
//												jsonBody.getString(column.getString(r)));
										body = new JSONObject(jsondata.get("contenttype").toString()).getString(lang)
												.replace(replaceData.getString(r),
														jsonBody.getString(column.getString(r)));
									} else
										body = body.replace(replaceData.getString(r),
												jsonBody.getString(column.getString(r)));

								}
							}
						} else {
							body = new JSONObject(jsondata.get("contenttype").toString()).getString(lang);
						}

					} else if (jsondata.getBoolean("custommail")) {
						body = getJson.has("AddcontentPre")
								? "<h2>Dear " + jsonBody.getString(getJson.getString("AddcontentPre")) + "</h2>"
										+ new JSONObject(jsondata.get("contenttype").toString()).getString(lang) + "<p>"
										+ DisplaySingleton.memoryApplicationSetting.getString("onboardurl") + "</p>"
								: new JSONObject(jsondata.get("contenttype").toString()).getString(lang);

					}
					List<String> emails = null;
					if (jsondata.getString("sentto").equalsIgnoreCase("Airport Admin")) {
						emails = new ArrayList();
						emails.add("nadim@eitworks.com");
					} else
						emails = Arrays.stream(email.split(",")).distinct().collect(Collectors.toList());
					for (int m = 0; m < emails.size(); m++) {
						int k = 0;
						do {
							k++;
							if (k == 3) {
								return resultOfMail = FAILED;
							}
							if (jsondata.getBoolean("withattachment") && !files.isEmpty()) {
								resultOfMail = sendEmailWithFile(smtpMail.getString("amazonverifiedfromemail"),
										emails.get(m),
//										new JSONObject(jsondata.get("subject").toString()).getString(lang), body,
										jsondata.getString("subject"), body, smtpMail.getString("amazonsmtpusername"),
										smtpMail.getString("amazonsmtppassword"),
										smtpMail.getString("amazonhostaddress"), smtpMail.getString("amazonport"),
										files);
							} else {
								resultOfMail = sendEmail(smtpMail.getString("amazonverifiedfromemail"), emails.get(m),
//										new JSONObject(jsondata.get("subject").toString()).getString(lang), body,
										jsondata.getString("subject"), body, smtpMail.getString("amazonsmtpusername"),
										smtpMail.getString("amazonsmtppassword"),
										smtpMail.getString("amazonhostaddress"), smtpMail.getString("amazonport"));
							}
						} while (!resultOfMail.equalsIgnoreCase("success"));
					}
				}
			}
		} catch (

		Exception e) {
			resultOfMail = FAILED;
			LOGGER.error("Exception at " + Thread.currentThread().getStackTrace()[1].getMethodName(), e);
		}

		return resultOfMail;

	}

	public String mailSender(JSONArray mail, String email, JSONObject getJson, JSONObject jsonBody, String name) {
		String resultOfMail = "";
		String body = "";
		JSONObject mailContent = null;
		try {
			String AdminMail = DisplaySingleton.memoryApplicationSetting.get("AdminMail").toString();
			String value = name.equalsIgnoreCase("Company Admin") ? "replacementContent"
					: name.equalsIgnoreCase("accepted") ? "replacementContentForAccepted"
							: "replacementContentForRejected";
			if (name.equalsIgnoreCase("Airport Officer")) {
				value = "replacementContent";
			}
			AmazonSMTPMail amazonSMTPMail = new AmazonSMTPMail();
			JSONObject smtpMail = new JSONObject(
					DisplaySingleton.memoryApplicationSetting.get("smptAmazonMail").toString());
			for (int i = 0; i < mail.length(); i++) {
				String maildata = mail.get(i) + ",Company Admin" + ":" + mail.get(i) + ",Airport Admin" + ":"
						+ mail.get(i) + ",Airport Officer";
				String[] getamailContent = maildata.split(":");
				for (int k = 0; k < getamailContent.length; k++) {
					if (!DisplaySingleton.memoryEmailCofig.has(getamailContent[k])) {
						continue;
					}
					mailContent = new JSONObject(
							DisplaySingleton.memoryEmailCofig.get(getamailContent[k].toString()).toString());
					if (getJson != null && getJson.has("Key")) {
						String key = generatekey();
						jsonBody.put("key", key);
						body = mailContent.getString("contenttype").replace("key", key);
					} else {
						if (getJson.has(value)) {
							if (new JSONObject(getJson.get(value).toString()).has("replace")) {
								JSONArray replaceData = new JSONObject(getJson.get(value).toString())
										.getJSONArray("replace");
								JSONArray column = new JSONObject(getJson.get(value).toString()).getJSONArray("column");
								for (int r = 0; r < replaceData.length(); r++) {
									if (r == 0)
										body = mailContent.getString("contenttype").replace(replaceData.getString(r),
												jsonBody.getString(column.getString(r)));
									else
										body = body.replace(replaceData.getString(r),
												jsonBody.getString(column.getString(r)));

								}
								if (jsonBody.has("pass")) {
									jsonBody.remove("pass");
								}
							}
						} else {
							body = mailContent.getString("contenttype");
						}
					}
					if (mailContent.getString("sentto").equalsIgnoreCase("Airport Admin")) {
						email = AdminMail;
					}
					int j = 1;
					do {
						j++;
						if (j == 3) {
							return resultOfMail = "Failed";
						}
//					for (int m = 0; m < emails.length; m++) {
						resultOfMail = amazonSMTPMail.sendEmail(smtpMail.getString("amazonverifiedfromemail"), email,
								mailContent.getString("subject"), body, smtpMail.getString("amazonsmtpusername"),
								smtpMail.getString("amazonsmtppassword"), smtpMail.getString("amazonhostaddress"),
								smtpMail.getString("amazonport"));
//					}

					} while (!resultOfMail.equalsIgnoreCase("success"));
				}
			}
		} catch (Exception e) {
			resultOfMail = "Failed";
			LOGGER.error("Exception at " + Thread.currentThread().getStackTrace()[1].getMethodName(), e);
		}

		return resultOfMail;

	}

	public String generatekey() {
		try {
			SecureRandom secureRandom = SecureRandom.getInstanceStrong();
			byte[] keyBytes = new byte[10];
			secureRandom.nextBytes(keyBytes);

			StringBuilder sb = new StringBuilder();
			for (byte b : keyBytes) {
				sb.append(String.format("%02X", b));
			}
			return sb.toString();
		} catch (Exception e) {
			LOGGER.error("generateKey", e);
		}
		return null;
	}

	public String sendEmail(String from, String to, String subject, String body, String smtpUser, String smtpPass,
			String host, String port, List<File> files) throws MessagingException {
		LOGGER.error("AmazonSMTPMail:: Entered:: for {}", to);
		try {
			Properties props = System.getProperties();
			props.put(MAILTRANSPORTPROTOCOL, "smtps");
			props.put(MAILSMTPPORT, port);
			props.put(MAILSMTPAUTH, "true");
			props.put(MAILSMTPSTARTTLSENABLE, "true");
			props.put(MAILSMTPSTARTTLSREQUIRED, "true");
			props.put(MAILSMTPSSLENABLE, "true");

			Session session = Session.getDefaultInstance(props);
			MimeMessage msg = new MimeMessage(session);

//			MimeMultipart multipart = new MimeMultipart();
			msg.setFrom(new InternetAddress(from, "Onboard-RAC"));
			msg.setRecipient(Message.RecipientType.TO, new InternetAddress(to));
			msg.setSubject(subject);

			MimeMultipart topLevelMultipart = new MimeMultipart();

			MimeBodyPart textPart = new MimeBodyPart();
			textPart.setContent(body, "text/html;charset=utf-8");
			topLevelMultipart.addBodyPart(textPart);

			MimeMultipart attachmentMultipart = new MimeMultipart();
			if (files != null) {
				for (File file : files) {
					MimeBodyPart attachmentPart = new MimeBodyPart();
					DataSource source = new FileDataSource(file);
					attachmentPart.setDataHandler(new DataHandler(source));
					attachmentPart.setFileName(file.getName());
					attachmentMultipart.addBodyPart(attachmentPart);
				}
			}

			MimeBodyPart attachmentMultipartPart = new MimeBodyPart();
			attachmentMultipartPart.setContent(attachmentMultipart);
			topLevelMultipart.addBodyPart(attachmentMultipartPart);

			msg.setContent(topLevelMultipart);

			transport = session.getTransport();
			transport.connect(host, smtpUser, smtpPass);
			transport.sendMessage(msg, msg.getAllRecipients());

			status = "Success";
		} catch (Exception e) {
			LOGGER.error("AmazonSMTPMail:: Entered:: Logger :: From = " + from + ", Port = " + port + ", host = " + host
					+ ", smtpUser = " + smtpUser + ", to = " + to + " ::", e);
			status = FAILED;
		} finally {
			transport.close();
		}
		return status;
	}

	public String sendEmail(String Email, String subject, String body) {
		String resultOfMail = "";
		try {
			JSONObject smtpMail = new JSONObject(
					DisplaySingleton.memoryApplicationSetting.get("smptAmazonMail").toString());
			int j = 1;
			do {
				j++;
				if (j == 3) {
					return resultOfMail = "Failed";
				}
				resultOfMail = sendEmail(smtpMail.getString("amazonverifiedfromemail"), Email, subject, body,
						smtpMail.getString("amazonsmtpusername"), smtpMail.getString("amazonsmtppassword"),
						smtpMail.getString("amazonhostaddress"), smtpMail.getString("amazonport"));
			} while (!resultOfMail.equalsIgnoreCase("success"));
		} catch (Exception e) {
			LOGGER.error("Exception at " + Thread.currentThread().getStackTrace()[1].getMethodName(), e);
		}
		return resultOfMail;
	}

	public String emailconfig(JSONObject email, JSONObject jsonbody, List<MultipartFile> files, String lang,
			String method) {
		String returndata = "";
		try {
			JSONArray mail = null;
			String mailid = "";
			String getcloumnname = "";
			if (!email.getBoolean("mailid")) {
				String geturl = pgrest + email.getString("api") + "?datas="
						+ jsonbody.getString(email.getString("column")) + "&name=" + email.getString("table");
				geturl = geturl.replace(" ", "%20");
				mailid = new JSONObject(dataTransmit.transmitDataspgrest(geturl).get(0).toString()).getString("mailid");
			} else {
				getcloumnname = new JSONObject(email.get("getcolumn").toString()).getString("columnname");
				mailid = jsonbody.getString(getcloumnname);
			}

			if (email.getString("getContentNameColumn").equalsIgnoreCase("default")
					&& email.getJSONArray("getContantType").toList().contains(method)) {
				int index = email.getJSONArray("getContantType").toList().indexOf(method);
				mail = email.getJSONObject("mail")
						.getJSONArray(email.getJSONArray("getContantType").get(index).toString());

			} else if (email.getJSONArray("getContantType").toList()
					.contains(jsonbody.get(email.getString("getContentNameColumn")))) {
				int index = email.getJSONArray("getContantType").toList()
						.indexOf(jsonbody.get(email.getString("getContentNameColumn")));
				mail = email.getJSONObject("mail")
						.getJSONArray(email.getJSONArray("getContantType").get(index).toString());

			} else {
				return returndata = "No Email Through";
			}

//			for (int i = 0; i < email.getJSONArray("getContantType").length(); i++) {
//				if (jsonbody.get(email.getString("getContentNameColumn"))
//						.equals(email.getJSONArray("getContantType").get(i))) {
//					mail = email.getJSONObject("mail")
//							.getJSONArray(email.getJSONArray("getContantType").get(i).toString());
//
//				} else
//					return returndata = "No Email Through";
//			}
			returndata = mailSender2(mail, mailid, email, jsonbody, files, lang);

		} catch (Exception e) {
			LOGGER.error("Exception at " + Thread.currentThread().getStackTrace()[0].getMethodName(), e);
		}

		return returndata;

	}

}