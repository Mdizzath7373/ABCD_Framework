package com.eit.abcdframework.controller;

import java.util.List;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.S3Object;
import com.eit.abcdframework.dto.CommonUtilDto;
import com.eit.abcdframework.serverbo.FileuploadServices;
import com.eit.abcdframework.service.DCDesignDataService;

@RestController
@RequestMapping("/DCDesignDataServlet")
@CrossOrigin("*")
public class DCDesignDataServlet {

	@Autowired
	DCDesignDataService dcDesignDataService;

	@Autowired
	FileuploadServices fileuploadServices;
	
	@Autowired
	AmazonS3 amazonS3;

	@Value("${applicationurl}")
	public String pgrest;

	private static final Logger LOGGER = LoggerFactory.getLogger("DCDesignDataServlet");

	@PostMapping(value = "/displaydata")
	public CommonUtilDto getDCDesignData(@RequestBody String data) {
		return dcDesignDataService.getDCDesignData(data);
	}

	@PostMapping(value = "/fileupload", produces = { "application/json" })
	public String fileupload(@RequestPart("files") List<MultipartFile> files, @RequestParam String data) {
		LOGGER.error("Entered into Fileupload");
		return dcDesignDataService.fileupload(files, data);
	}

	@PostMapping(value = "/getwidgets", produces = { "application/json" })
	public String getwidgets(@RequestBody String data) {
		return dcDesignDataService.getwidgetsdata(data);
	}

	@PostMapping(value = "/fileuploadforgeneratedpdf", produces = { "application/json" })
	public String fileuploadforgeneratedpdf(@RequestParam String file, @RequestParam String data) {
		JSONObject json = new JSONObject(file);
		String base64 = json.getString("file");
		return dcDesignDataService.fileuploadforgeneratedpdf(base64, data);
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
	public String test(@RequestPart("files") MultipartFile files, @RequestParam String data) throws Exception {
		return dcDesignDataService.fileuploadwithprogress(files, data);
//		return fileuploadServices.convertPdfToMultipart(files, data);
	}

	@PostMapping("/progress")
	public ResponseEntity<String> getProgress(@RequestBody String data) {
		dcDesignDataService.getProgress(data);
		return ResponseEntity.ok(new JSONObject().put("reflex", "Success").toString());
	}

	@PostMapping("/mergeToPDF")
	public ResponseEntity<String> mergeToPDF(@RequestPart("files") MultipartFile files, @RequestParam String data) {
		return ResponseEntity.ok(dcDesignDataService.mergeToPDF(files, data));
	}

	@PostMapping("/UpdatePDFImage")
	public ResponseEntity<String> UpdatePDFImage(@RequestBody String data) {
		return ResponseEntity.ok(dcDesignDataService.SplitterPDFChanges(data));
	}

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
