package com.eit.abcdframework.controller;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.eit.abcdframework.dto.CommonUtilDto;
import com.eit.abcdframework.serverbo.CommonServices;
import com.eit.abcdframework.serverbo.DisplaySingleton;
import com.eit.abcdframework.serverbo.FileuploadServices;
import com.eit.abcdframework.service.DCDesignDataService;
import com.eit.abcdframework.service.FormdataService;
import com.eit.abcdframework.util.AmazonSMTPMail;

@RestController
@RequestMapping("/DCDesignDataServlet")
@CrossOrigin("*")
public class DCDesignDataServlet {

	@Autowired
	DCDesignDataService dcDesignDataService;

	@Autowired
	FileuploadServices fileuploadServices;
	
	@Autowired
	FormdataService formdataService;
	
	@Autowired
	CommonServices commonServices;
	
	@Autowired
	AmazonSMTPMail amazonSMTPMail;

//	@Value("${applicationurl}")
//	public String pgrest;

	private static final Logger LOGGER = LoggerFactory.getLogger("DCDesignDataServlet");
	private static JSONObject email = null;
	
	@PostMapping(value = "/getGrid")
	public String getNewGridJSON(@RequestBody String datas) {
		return dcDesignDataService.getNewGridJSON(datas);
	}

	@PostMapping(value = "/displaydata")
	public CommonUtilDto getDCDesignData(@RequestBody String data) {
		return dcDesignDataService.getDCDesignData(data);
	}
	
	
	@PostMapping(value = "/displaydata1")
	public CommonUtilDto getDCDesignData1(@RequestBody String data) {
		return dcDesignDataService.getDCDesignData(data);
	}
	
	
	@PostMapping(value = "/displayMultiGrid")
	public String getMultipleGrids(@RequestBody String data) {
	return dcDesignDataService.multiGridDesignData(data);
	}

	@PostMapping(value = "/fileupload", produces = { "application/json" })
	public String fileupload(@RequestPart("files") List<MultipartFile> files, @RequestParam String data) {
		LOGGER.error("Entered into Fileupload");
		return dcDesignDataService.fileUpload(files, data,"Upload");
	}

	@PostMapping(value = "/getwidgets", produces = { "application/json" })
	public String getwidgets(@RequestBody String data) {
		return dcDesignDataService.getwidgetsdata(data);
	}


	@PostMapping(value = "/singlefileupload", produces = { "application/json" })
	public String fileupload(@RequestPart("files") MultipartFile files) {
		LOGGER.error("Entered into Fileupload");
		return fileuploadServices.fileupload(files);
	}

	@PostMapping(value = "/getchartDesignData")
	public String getDCDesignChart(@RequestBody String data) {
		return dcDesignDataService.getDCDesignChart(data);
	}

	@PostMapping("/fileuploadwithprogress")
	public String test(@RequestPart("files") List<MultipartFile> files, @RequestParam String data)  {
		return dcDesignDataService.fileUpload(files, data,"UploadWithProgress");
//		return fileuploadServices.convertPdfToMultipart(files, data);
	}

	@PostMapping("/progress")
	public ResponseEntity<String> getProgress(@RequestBody String data) {
		dcDesignDataService.getProgress(data);
		return ResponseEntity.ok(new JSONObject().put("reflex", "Success").toString());
	}

	@PostMapping("/mergeToPDF")
	public ResponseEntity<String> mergeToPDF(@RequestPart("files") List<MultipartFile> files, @RequestParam String data) {
		return ResponseEntity.ok(dcDesignDataService.fileUpload(files, data,"MergeFile"));
	}

	@PostMapping("/UpdatePDFImage")
	public ResponseEntity<String> UpdatePDFImage(@RequestBody String data) throws JSONException, Exception {
		if (data.equalsIgnoreCase("") && !data.startsWith("{")) {
			return ResponseEntity.ok(new JSONObject().put("error", "Please Check Your Data Object!").toString());
		}
		JSONObject jsonObject1;
		if (!data.startsWith("{"))
			jsonObject1 = new JSONObject(CommonServices.decrypt(data));
		else
			jsonObject1 = new JSONObject(data);
		return ResponseEntity.ok(dcDesignDataService.SplitterPDFChanges(jsonObject1));
	}
	
	
	@PostMapping("/UploadImage")
	public ResponseEntity<String> uploadProcess(@RequestPart("files") List<MultipartFile> files,@RequestParam String data ) throws JSONException, Exception {
		return ResponseEntity.ok(dcDesignDataService.uploadImageProgress(files,data));
		
	}

	@PostMapping(value = "/inspectionemail", produces = { "application/json" })
	public String inspectionemail(@RequestPart("files") List<MultipartFile> files, @RequestParam String data) throws JSONException, Exception {
	    LOGGER.error("Entered into inspectionemail");

	    try {
	        // Input validation
	        if (data == null || data.equalsIgnoreCase("")) {
	            LOGGER.error("Data validation failed - empty or null data");
	            return "Please Check Your Data Object!";
	        }

	        JSONObject jsonObject1;
	        JSONObject gettabledata;
	        
	        // Parse JSON with error handling
	        try {
	            if (!data.startsWith("{")) {
	                jsonObject1 = new JSONObject(CommonServices.decrypt(data));
	            } else {
	                jsonObject1 = new JSONObject(data);
	            }
	            LOGGER.error("JSON parsing successful");
	        } catch (JSONException e) {
	            LOGGER.error("JSON parsing error: {}", e.getMessage());
	            return "Invalid JSON data format";
	        }

	        // Extract header and body with error handling
	        JSONObject jsonheader;
	        JSONObject jsonbody;
	        
	        try {
	            jsonheader = jsonObject1.has("PrimaryBody")
	                ? new JSONObject(jsonObject1.getJSONObject("PrimaryBody").getJSONObject("header").toString())
	                : new JSONObject(jsonObject1.getJSONObject("header").toString());

	            jsonbody = jsonObject1.has("PrimaryBody")
	                ? new JSONObject(jsonObject1.getJSONObject("PrimaryBody").getJSONObject("body").toString())
	                : new JSONObject(jsonObject1.getJSONObject("body").toString());
	            
	            LOGGER.error("Header and body extraction successful");
	        } catch (JSONException e) {
	            LOGGER.error("Error extracting header/body: {}", e.getMessage());
	            return "Invalid JSON structure";
	        }

	        String name = "Inspection";
	        String primary = jsonbody.getString("inspectionsrno");
	        LOGGER.error("enter in to inspectionid:{}", primary);
	        String inspectiondata;
	        
	        // Service call with error handling
	        try {
	            inspectiondata = formdataService.transmittingToMethod("GET", name, primary, "", false, false);
	            LOGGER.error("Service call successful");
	        } catch (Exception e) {
	            LOGGER.error("Service call failed: {}", e.getMessage());
	            return "Failed to retrieve inspection data";
	        }

	        JSONObject displayConfig;
	        JSONObject jsonResponse = new JSONObject(inspectiondata);
	        JSONArray datavalueArray = jsonResponse.getJSONArray("datavalue");
	        JSONObject inspectionObject = datavalueArray.getJSONObject(0);
	        
	        if (jsonbody.has("s3link")) {
	            inspectionObject.put("s3link", jsonbody.getJSONArray("s3link"));
	        }
	        inspectionObject.put("status", "Submitted");
	        // Get email config with error handling
	        try {
	            displayConfig = DisplaySingleton.memoryDispObjs2.getJSONObject("Inspectionmail");
	            gettabledata = new JSONObject(displayConfig.get("datas").toString());
	            email = new JSONObject(gettabledata.get("email").toString());
	            LOGGER.error("Email configuration loaded successfully");
	        } catch (Exception e) {
	            LOGGER.error("Email config error: {}", e.getMessage());
	            return "Email configuration not found";
	        }

	        List<File> filedata = new ArrayList<>();

	        // File processing with error handling
	        for (MultipartFile mfile : files) {
	            try {
	                if (mfile.isEmpty()) {
	                    LOGGER.warn("Empty file skipped: {}", mfile.getOriginalFilename());
	                    continue;
	                }
	                
	                File file = new File(System.getProperty("java.io.tmpdir"), mfile.getOriginalFilename());
	                mfile.transferTo(file);
	                filedata.add(file);
	                LOGGER.error("File processed successfully: {}", mfile.getOriginalFilename());
	            } catch (Exception e) {
	                LOGGER.error("File processing error for {}: {}", mfile.getOriginalFilename(), e.getMessage());
	            }
	        }

	        // Check if any files were processed
	        if (filedata.isEmpty()) {
	            LOGGER.error("No files could be processed");
	            return "No files could be processed";
	        }

	        
	        // Send email with error handling
	        try {
	            LOGGER.error("Started the mail sending process");

	            amazonSMTPMail.emailconfig(email, inspectionObject, filedata,
	                jsonheader.has("lang") ? jsonheader.getString("lang") : "en", "POST",
	                gettabledata.getString("schema"));
	            
	        } catch (Exception e) {
	            LOGGER.error("Email sending failed: {}", e.getMessage(), e);
	            return "Failed to send email";
	        }

	        return "Email sent successfully";
	        
	    } catch (Exception e) {
	        LOGGER.error("Unexpected error in inspectionemail: {}", e.getMessage(), e);
	        return "An error occurred while processing the request";
	    }
	}
//	@PostMapping(value = "/testfileupload", produces = { "application/json" })
//	public String testfileupload(@RequestPart("files") List<MultipartFile> files) {
//		LOGGER.error("Entered into Fileupload ---------{}", Instant.now());
//		return String.valueOf(Instant.now());
//	}

//	@PostMapping("/test")
//	public ResponseEntity<String> test(@RequestPart("files") MultipartFile files) {
////		dcDesignDataService.mergeToPDF(files,data);
//        fileuploadServices.test(files);
//		return null;		
//	}

//	final HttpClient httpClient = HttpClient.newHttpClient();

//	@PostMapping("/test") 
	// Method that triggers an API call and periodically polls for progress
//    public void startApiCallAndPoll(String apiUrl) {
//		
//        // Trigger API call
//        triggerApi(apiUrl);
//
//        // Schedule polling for progress
//        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
//        ScheduledFuture<?> scheduledFuture = scheduler.scheduleAtFixedRate(() -> {
//            try {
//                // Poll progress API
//                String progress = pollProgress(apiUrl + "/progress");
//                System.out.println("Progress: " + progress);
//
//                // Terminate polling if the task is completed
//                if ("100%".equals(progress)) {
//                    System.out.println("Task completed.");
//                    scheduler.shutdown();
//                }
//            } catch (Exception e) {
//                e.printStackTrace();
//                scheduler.shutdown();
//            }
//        }, 0, 5, TimeUnit.SECONDS);  // Poll every 5 seconds
//    }
//
//    // Method to trigger the API call
//    private void triggerApi(String apiUrl) {
//        try {
//            HttpRequest request = HttpRequest.newBuilder()
//                    .uri(URI.create(apiUrl))
//                    .timeout(Duration.ofSeconds(10))
//                    .POST(HttpRequest.BodyPublishers.noBody())
//                    .build();
//
//            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
//            System.out.println("API Triggered: " + response.body());
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//
//    // Method to poll for progress
//    private String pollProgress(String progressUrl) throws Exception {
//        HttpRequest request = HttpRequest.newBuilder()
//                .uri(URI.create(progressUrl))
//                .timeout(Duration.ofSeconds(10))
//                .GET()
//                .build();
//
//        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
//        return response.body();  // Return progress data (e.g., "50%", "100%")
//    }
}
