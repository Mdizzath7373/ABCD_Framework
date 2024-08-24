package com.eit.abcdframework.cron;


import java.awt.Desktop;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.eit.abcdframework.http.caller.Httpclientcaller;
import com.eit.abcdframework.serverbo.DisplaySingleton;
import com.eit.abcdframework.util.AmazonSMTPMail;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfDiv;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

import jakarta.mail.MessagingException;
@Service
public class EmailPreferenceService {
	
	private static final Logger LOGGER = LoggerFactory.getLogger("DisplaySingleton");
	@Autowired
	Httpclientcaller dataTransmit;
	
	@Autowired
	AmazonSMTPMail amazonsmtpmail;
//	@Value("${applicationurl}")
	private static String applicationurl = "http://localhost:3000/rpc/";
	
	
	public String getCompanyEmailPre() {
		try {
			String url = applicationurl + "getcompanyemailpre";
			JSONArray getArrayObj = dataTransmit.transmitDataspgrest(url);
			for (int i = 0; i < getArrayObj.length(); i++) {
				JSONObject json = new JSONObject(getArrayObj.get(i).toString());
				System.out.println(json);
				if( !json.isNull("emailpreference") && !json.isNull("fax")) {
					System.out.println(json.get("emailpreference"));
					
					Object emailPreferenceValue = json.get("emailpreference");
					String emailid = json.getString("fax");
					if (emailPreferenceValue instanceof String) {
						
			            String emailPreferenceString = (String) emailPreferenceValue;
			            JSONObject emailconfigjson = new JSONObject(emailPreferenceString);
			            JSONObject durwhere = emailconfigjson.getJSONObject("staticdurationwhere");
			            String[] valueTypes = {"warning", "recentcomments", "activitylogs"};

			            for (String valueType : valueTypes) {
			                String value = fetchStringValue(emailconfigjson, valueType, valueType+"value");
			                JSONArray field = fetchStringField(emailconfigjson, valueType, valueType+"field");

			                processValue(value,valueType,json.getString("companyname"),emailid,durwhere);
			            }
			        }
				}
			}

		} catch (Exception e) {
			LOGGER.error("Exception in getconfigsObj : ", e);
		}
		return "Success";
	}
	private String fetchStringValue(JSONObject jsonObject, String outerKey, String innerKey) {
	    return jsonObject.getJSONObject(outerKey).getString(innerKey);
	}
	
	private JSONArray fetchStringField(JSONObject jsonObject, String outerKey, String innerKey) {
	    return jsonObject.getJSONObject(outerKey).getJSONArray(innerKey);
	}
	private void processValue(String value, String valueType, String companyname, String emailid,JSONObject durwhere) throws JSONException, MessagingException, IOException, DocumentException {
		JSONObject smtpMail = new JSONObject(
				DisplaySingleton.memoryApplicationSetting.get("smptAmazonMail").toString());
		String result;
		String body;
		String durationwhere;
		List<File> mailPdf = null;
	    switch (value.toLowerCase()) {
	        case "instantly":
	        	
	            break;
	        case "daily":
	        	 durationwhere = durwhere.getString("yesterdaywhere");
	        	 body = writeContent(valueType,"","",durationwhere,companyname);
	        	 if (body != null && !body.equals("[]") && !body.isEmpty()) {
        		 mailPdf = writePdffile(valueType, body);
	        	 result = amazonsmtpmail.sendEmail(smtpMail.getString("amazonverifiedfromemail"), emailid,
						"Onboard "+value+" "+valueType+" Updates","find your file", smtpMail.getString("amazonsmtpusername"),
						smtpMail.getString("amazonsmtppassword"), smtpMail.getString("amazonhostaddress"),
						smtpMail.getString("amazonport"),mailPdf);
	        	System.out.println(result+"-------mailsending");
        		 }
	            break;
	        case "weekly":
	        	 if (isLastDayOfWeek()) {
	             durationwhere = durwhere.getString("lastweekwhere");
	             body = writeContent(valueType,"","",durationwhere,companyname);
	             if (body != null &&!body.equals("[]") && !body.isEmpty()) {
	             mailPdf = writePdffile(valueType, body);
	        	 result = amazonsmtpmail.sendEmail(smtpMail.getString("amazonverifiedfromemail"), emailid,
	        			 "Onboard "+value+" "+valueType+" Updates","find your file", smtpMail.getString("amazonsmtpusername"),
	 						smtpMail.getString("amazonsmtppassword"), smtpMail.getString("amazonhostaddress"),
	 						smtpMail.getString("amazonport"),mailPdf);
	 	        	System.out.println(result+"-------mailsending");
	             }
	             } else {
//	            	 
	             }
	            break;
	        case "monthly":
	        	 if (isLastDayOfMonth()) {
	        		 durationwhere = durwhere.getString("lastmonthwhere");
	        		 body = writeContent(valueType,"","",durationwhere,companyname);
	        		 if (body != null &&!body.equals("[]") && !body.isEmpty()){
	        		 mailPdf = writePdffile(valueType, body);
	        		 result = amazonsmtpmail.sendEmail(smtpMail.getString("amazonverifiedfromemail"), emailid,
		        			 "Onboard "+value+" "+valueType+" Updates","find your file", smtpMail.getString("amazonsmtpusername"),
		 						smtpMail.getString("amazonsmtppassword"), smtpMail.getString("amazonhostaddress"),
		 						smtpMail.getString("amazonport"),mailPdf);
		 	        	System.out.println(result+"-------mailsending");
	        		 }
	             } else {
	            	 
	             }
	            break;
	        default:
	        	
	            break;
	    }
	}
	
	public String writeContent(String contenttype, String fields, String value, String duration,String companyname) throws IOException {
		String url = applicationurl;
		JSONArray getArrayObj = null;
		if(contenttype == "warning") {
			return "";
		}else if(contenttype == "recentcomments") {
			url = url +"getcommentsforemail?in_companyname="+companyname+"&"+duration;
			getArrayObj = dataTransmit.transmitDataspgrest(url);
			System.out.println(getArrayObj);
			return getArrayObj.toString();
		}else if(contenttype == "activitylogs") {
			url = url +"getactivityforemail?in_companyname="+companyname+"&"+duration;
			getArrayObj = dataTransmit.transmitDataspgrest(url);
			System.out.println(getArrayObj);
			return getArrayObj.toString();
		}else {
			return "";
		}
		
	}
	
	public String sendemailpreferencemail() {
		return "";
		
	}
	
	private boolean isLastDayOfWeek() {
	    LocalDate today = LocalDate.now();
	    return today.getDayOfWeek() == DayOfWeek.SUNDAY;
	}

	private boolean isLastDayOfMonth() {
	    LocalDate today = LocalDate.now();
	    int lastDayOfMonth = today.lengthOfMonth();
	    return today.getDayOfMonth() == lastDayOfMonth;
	}
	
	public List<File> writePdffile(String title, String data) throws FileNotFoundException, DocumentException {
		LOGGER.info("Entering Send EMAIL REPORT");
		

		Font titleFont = FontFactory.getFont(FontFactory.COURIER_BOLD, 15, BaseColor.BLACK);
		Font headerFont = FontFactory.getFont(FontFactory.COURIER_BOLD, 8, BaseColor.BLACK);
		Font cellFont = FontFactory.getFont(FontFactory.TIMES_ROMAN, 6, BaseColor.BLACK);
		final String FONT = "KacstNaskh.ttf";
		Font f = FontFactory.getFont(FONT, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
		Document document = new Document();
		document.setMargins(0, 0, 5, 5);
		PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(title + ".pdf"));
		document.open();
//		String logoURl = "http://" + provider.getBucketName() + ".s3.amazonaws.com/Company/"
//				+ provider.getLogoUrl();
//		Image img = Image.getInstance(logoURl);
//		img.setAlignment(Element.ALIGN_CENTER);
//		img.scaleToFit(250, 100);
		Paragraph p1 = new Paragraph("", titleFont);
		PdfDiv pdfDiv = new PdfDiv();
		p1.add(new Chunk(title.toUpperCase(), f));
		p1.setAlignment(Element.ALIGN_CENTER);
		pdfDiv.setRunDirection(PdfWriter.RUN_DIRECTION_LTR);
		pdfDiv.addElement(p1);
//		String[] mail;
		JSONArray json = new JSONArray(data);
		JSONObject headerObject = json.getJSONObject(0);
		
//		JSONObject headerObject = jsonarray.getJSONObject(0);	
		List<String> perfectKeys = new ArrayList<>();

		PdfPTable table = new PdfPTable(headerObject.length());

		for (String headerKey : headerObject.keySet()) {
			perfectKeys.add(headerKey);
			Paragraph cellParagraph = new Paragraph("", headerFont);
			PdfDiv cellDiv = new PdfDiv();
			cellParagraph.add(new Chunk(headerKey.toUpperCase(), f));
			cellDiv.setRunDirection(PdfWriter.RUN_DIRECTION_LTR);
			cellDiv.addElement(cellParagraph);
			
			PdfPCell cell = new PdfPCell();
			cell.addElement(cellDiv);
			cell.setBackgroundColor(BaseColor.LIGHT_GRAY);
			cell.setHorizontalAlignment(Element.ALIGN_CENTER);
			cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
			table.addCell(cell);
		}

		for (int i = 0; i < json.length(); i++) {
			JSONObject obj = json.getJSONObject(i);
			for (int j = 0; j < perfectKeys.size(); j++) {
				String cellContent = obj.optString(perfectKeys.get(j));
				Paragraph innerCellParagraph = new Paragraph("", cellFont);
				PdfDiv innerCellDiv = new PdfDiv();
				innerCellParagraph.add(new Chunk(cellContent, f));
				innerCellDiv.setRunDirection(PdfWriter.RUN_DIRECTION_LTR);
				innerCellDiv.addElement(innerCellParagraph);
				PdfPCell cell = new PdfPCell();
				cell.addElement(innerCellDiv);
				table.addCell(cell);
			}
		}
//		document.add(img);
		document.add(pdfDiv);
		document.add(Chunk.NEWLINE);
		document.add(table);
		document.close();
		 
		File xls = new File(title + ".pdf");
		System.out.println("Success");
		
		
		List<File> xlsList = new ArrayList<>();
		xlsList.add(xls);
		return xlsList;
	}
}

