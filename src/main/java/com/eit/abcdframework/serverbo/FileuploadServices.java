package com.eit.abcdframework.serverbo;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.zip.GZIPOutputStream;

import javax.imageio.ImageIO;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
//import org.apache.pdfbox.pdmodel.PDDocument;
//import org.apache.pdfbox.rendering.PDFRenderer;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import com.eit.abcdframework.config.ConfigurationFile;
import com.eit.abcdframework.http.caller.Httpclientcaller;
import com.eit.abcdframework.s3bucket.S3Upload;
import com.eit.abcdframework.websocket.WebSocketService;

@Component
public class FileuploadServices {

	private static final Logger LOGGER = LoggerFactory.getLogger("FileuploadServices");
	private static SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy hh:mm:ss");

	@Value("${execeptFileTypes}")
	private String execeptFileTypes;

	@Autowired
	S3Upload s3Upload;

	@Autowired
	Httpclientcaller daHttpclientcaller;

	@Autowired
	WebSocketService socketService;

	@Value("${applicationurl}")
	private String pgresturl;

	public static String s3url = ConfigurationFile.getStringConfig("s3bucket.url");

	private String path = ConfigurationFile.getStringConfig("s3bucket.path");

	private static Map<String, AtomicInteger> progress = new HashMap<>();

	public void setProgress(Map<String, AtomicInteger> progress) {
		this.progress = progress;
	}

	public Map<String, AtomicInteger> getProgress() {
		return progress;
	}

	public JSONObject fileupload(JSONObject gettabledata, List<MultipartFile> files, JSONObject jsonbody,
			JSONObject documentdata) {
		JSONObject oldFile = new JSONObject();
		try {
			List<String> prefilename = new ArrayList<>();
			// filepathname was define which column value was set on file path
			JSONArray jarr = new JSONArray(gettabledata.getJSONArray("filepathname").toString());

			// set list of column name in json body (column name)
			JSONArray column = new JSONArray(gettabledata.getJSONArray("column").toString());
			String filename = "";

			for (int i = 0; i < jarr.length(); i++) {
				filename += jsonbody.get(jarr.get(i).toString());
			}
			for (int i = 0; i < files.size(); i++) {
				if (files.get(i).getOriginalFilename().split("\\.")[1] != null && new JSONArray(execeptFileTypes)
						.toList().contains(files.get(i).getOriginalFilename().split("\\.")[1])) {
					return new JSONObject().put("error", "Please upload a vaild file format!");
				}
				if (files.get(i).getOriginalFilename() != null
						&& !files.get(i).getOriginalFilename().equalsIgnoreCase("File name")
						&& !files.get(i).getOriginalFilename().equalsIgnoreCase("")) {
					// Get File Name.
					StringBuilder getfilename = new StringBuilder(files.get(i).getOriginalFilename().split("\\.")[0]);
					// Split into Name only Remove Annoying Data.
					String name = "";
					if (getfilename.toString().contains("$$")) {
						name = getfilename.toString().split("\\$\\$")[0];
					} else {
						name = getfilename.deleteCharAt(getfilename.length() - 1).toString();
					}
					// Then Set FilePath Name.
					String[] extensen = files.get(i).getOriginalFilename().split("\\.");
					String filePath = path + filename + i + dateFormat.format(new Date()) + "."
							+ extensen[extensen.length - 1];
					// Start to Upload File in S3Bucket.
					if (uploadFile(files.get(i), filePath)) {
						String path = s3url + filePath;
						// Set the Url to Table column.
						if (jsonbody.has(column.get(0).toString())
								&& !jsonbody.get(column.get(0).toString()).equals(null)) {
							JSONObject json = new JSONObject(jsonbody.get(column.get(0).toString()).toString());
							if (!documentdata.isEmpty()) {
								JSONObject setDocumentData = new JSONObject(
										documentdata.getJSONObject(name).toString());
								setDocumentData.put(files.get(i).getOriginalFilename(), path);
								json.put(name, setDocumentData);
								prefilename.add(name);
								oldFile.put(files.get(i).getOriginalFilename(), path);
							} else {
								if (json.has(name)) {
									JSONObject getjson = json.getJSONObject(name);
									getjson.put(files.get(i).getOriginalFilename(), path);
									json.put(name, getjson);
								} else {
									JSONObject getjson = new JSONObject();
									getjson.put(files.get(i).getOriginalFilename(), path);
									json.put(name, getjson);
								}
							}
							jsonbody.put(column.get(0).toString(), json);
						} else {
							JSONObject json = new JSONObject();
							if (!documentdata.isEmpty()) {
								JSONObject setDocumentData = new JSONObject(
										documentdata.getJSONObject(name).toString());
								setDocumentData.put(files.get(i).getOriginalFilename(), path);
								json.put(name, setDocumentData);
							} else {
								JSONObject getjson = new JSONObject();
								getjson.put(files.get(i).getOriginalFilename(), path);
								json.put(name, getjson);
							}
							jsonbody.put(column.get(0).toString(), json);
						}
					} else {
						throw new Exception("Exception In Adding Document");
					}
				}
			}
		} catch (

		Exception e) {
			LOGGER.error("fileUpload classS -->{}", e);
			jsonbody = new JSONObject().put("error", e.getMessage());
		}
		return jsonbody;

	}

	public boolean uploadFile(MultipartFile mFile, String path) throws Exception {
		File file = convertMultiPartFileToFile(mFile);
//		S3Upload s3Upload = new S3Upload();
		if (s3Upload.NewuploadFile(ConfigurationFile.getStringConfig("s3bucket.bucketName").toString(), path, file,
				true)) {
			file.delete();
			return true;
		} else {
			return false;
		}
	}

	private File convertMultiPartFileToFile(MultipartFile mFile) {
		File convFile = new File(mFile.getOriginalFilename());
		try {
			convFile.createNewFile();
			FileOutputStream fos = new FileOutputStream(convFile);
			fos.write(mFile.getBytes());
			fos.close();
		} catch (IOException e) {
			LOGGER.error("Excepton at : ", e);
		}
		return convFile;
	}

	public String fileupload(MultipartFile files) {
//		String path = "";
		JSONArray returnMessage = new JSONArray();
		try {

			String[] extensen = files.getOriginalFilename().split("\\.");
			String filename = files.getOriginalFilename().split("\\.")[0];
			System.out.println(path);
			String filePath = path + "/" + filename + dateFormat.format(new Date()) + "."
					+ extensen[extensen.length - 1];
			if (uploadFile(files, filePath)) {
				String paths = s3url + filePath;
				returnMessage.put(new JSONObject().put("datavalues", paths));
			}
		} catch (Exception e) {
			LOGGER.error("Excepton at : ", e);
		}
		return returnMessage.toString();

	}

	public JSONObject meth() {
		JSONObject json = new JSONObject();
		try {
			Field changeMap = json.getClass().getDeclaredField("map");
			changeMap.setAccessible(true);
			changeMap.set(json, new TreeMap<>());
			changeMap.setAccessible(false);
		} catch (IllegalAccessException | NoSuchFieldException e) {
			LOGGER.info(e.getMessage());
		}
//			JSONObject getjson = new JSONObject();
//			try {
//				Field changeMap = json.getClass().getDeclaredField("map");
//				changeMap.setAccessible(true);
//				changeMap.set(json, new TreeMap<>());
//				changeMap.setAccessible(false);
//			} catch (IllegalAccessException | NoSuchFieldException e) {
//				LOGGER.info(e.getMessage());
//			}
		return json;
	}

//	public Map<String, String> convertPdfToMultipart(MultipartFile multipartFile) throws Exception {
//		Map<String, String> data = new HashMap<>();
//		try (InputStream inputStream = multipartFile.getInputStream();
//				PDDocument document = PDDocument.load(inputStream)) {
//
//			PDFRenderer pdfRenderer = new PDFRenderer(document);
//			int pageCount = document.getNumberOfPages();
//
//			for (int pageIndex = 0; pageIndex < pageCount; pageIndex++) {
//				BufferedImage bufferedImage = pdfRenderer.renderImageWithDPI(pageIndex, 300); // 300 DPI for high
//																								// quality
//				File imageFile = new File("page_" + (pageIndex + 1) + ".png"); // Output file name
//				
//					ImageIO.write(bufferedImage, "PNG", imageFile);// Save as PNG image
//				System.err.println(imageFile.getAbsolutePath());
//					String filePath = path + "page_" + (pageIndex + 1) + dateFormat.format(new Date()) + "." + "png";
//					if (uploadFile(imageFile, filePath)) {
//						String path = s3url + filePath;
//						data.put("page_" + (pageIndex + 1), path);
//					}
//				System.out.println("Page " + (pageIndex + 1) + " saved as " + imageFile.getAbsolutePath());
//			}
//		}

//		return data;
//	}

	public String convertPdfToMultipart(MultipartFile multipartFile, String primaryKey, String id, JSONObject jsonbody)
			throws IOException {

		ExecutorService executorService = Executors.newFixedThreadPool(20);

		int maxRetries = 3; // Retry once
		int retryDelayMillis = 2000; // Delay between retries in milliseconds
		int pageCount = 0;
		Instant startTime = Instant.now(); // Record start time
		AtomicInteger progressCount = new AtomicInteger(0);
		final Map<String, String> base64String = new TreeMap<>(); // Initializing the TreeMap
		List<Integer> FaildPages = new ArrayList<>();

//        PDDocument document = null;
		try (InputStream inputStream = multipartFile.getInputStream();
				PDDocument document = PDDocument.load(inputStream)) {
			PDFRenderer pdfRenderer = new PDFRenderer(document);
			pageCount = document.getNumberOfPages();
			AtomicInteger preProgresCount = new AtomicInteger(0);
			List<Future<Boolean>> futures = new ArrayList<>();
			for (int pageIndex = 0; pageIndex < pageCount; pageIndex++) {
				int currentIndex = pageIndex;
				futures.add(executorService.submit(() -> processPage(document, pdfRenderer, currentIndex, base64String,
						maxRetries, retryDelayMillis, primaryKey, progressCount, FaildPages, id, jsonbody,
						preProgresCount)));
			}
			// Wait for all tasks to complete
			for (Future<Boolean> future : futures) {
				try {
					future.get();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		} finally {
//            if (document != null) {
//                document.close(); // Close the document after all threads are done
//            }
			executorService.shutdown();
			try {
				if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
					executorService.shutdownNow();
				}
			} catch (InterruptedException ex) {
				executorService.shutdownNow();
				Thread.currentThread().interrupt();
			}
		}
//		if(pageCount!=base64String.size()) {
//			
//		}
		LOGGER.warn("ENTER INTO saveOrUpdateData {BASE64iMAGES}---------->" + Instant.now());
		saveOrUpdateData(base64String, primaryKey, progressCount, id, jsonbody);
		LOGGER.warn("EXIT saveOrUpdateData {BASE64iMAGES}------->" + Instant.now());

		Instant endTime = Instant.now(); // Record end time
		Duration duration = Duration.between(startTime, endTime);
		LOGGER.warn(startTime + "_" + endTime + " Processing time: " + duration.toMillis() + " milliseconds");

		return "Success";
	}

	private boolean processPage(PDDocument document, PDFRenderer pdfRenderer, int pageIndex,
			Map<String, String> base64String, int maxRetries, int retryDelayMillis, String primaryKey,
			AtomicInteger progressCount, List<Integer> FaildPages, String id, JSONObject jsonbody,
			AtomicInteger preProgresCount) {
		for (int attempt = 0; attempt <= maxRetries; attempt++) {
			try {

				synchronized (document) {
					BufferedImage bufferedImage = pdfRenderer.renderImageWithDPI(pageIndex, 150);
					ByteArrayOutputStream baos = new ByteArrayOutputStream();
					ImageIO.write(bufferedImage, "JPEG", baos);

					base64String.put("page_" + (pageIndex + 1),
							"data:image/png;base64,"+Base64.getEncoder().encodeToString(baos.toByteArray()).replaceAll("=+$", ""));

					int totalPages = document.getNumberOfPages();
					int calculatedProgress = (pageIndex + 1) * 75 / totalPages;

					if (pageIndex + 1 == totalPages) {
						progressCount.set(75);
						preProgresCount.set(75);
						progress.put(id + "-" + primaryKey, progressCount);
					} else {
						progressCount.set(calculatedProgress);

						progress.put(id + "-" + primaryKey, progressCount);
					}
					System.err.println(progressCount);
					if (progressCount.get() > preProgresCount.get() || progressCount.get() == 75) {
						preProgresCount.set(progressCount.get());
						String socketRes = socketService.pushSocketData(new JSONObject(), jsonbody, "progress");
					}
				}
				LOGGER.warn("DONE page " + (pageIndex + 1));

				// After processing every 100 pages, save or update the PDF data
				if ((pageIndex + 1) % 100 == 0) {
					// Sleep after the save/update completes
					LOGGER.warn("Sleeping after processing " + (pageIndex + 1) + " pages.");
					Thread.sleep(300000); // Sleep for the specified duration
				}
				return true;
			} catch (IllegalStateException e) {
				LOGGER.error("Attempt " + (attempt + 1) + " failed for page " + (pageIndex + 1) + ": ", e);
				if (attempt < maxRetries) {
					try {
						Thread.sleep(retryDelayMillis);
					} catch (InterruptedException ie) {
						Thread.currentThread().interrupt();
					}
				} else {
					LOGGER.error("Failed to process page " + (pageIndex + 1) + " after " + (attempt + 1) + " attempts.",
							e);
					return false; // Return null to indicate failure
				}
			} catch (Exception e) {
				FaildPages.add((pageIndex + 1));
				LOGGER.error("Error rendering page " + (pageIndex + 1), e);
				return false; // Return null to indicate failure
			}
		}
		return false; // Should not reach here
	}

	private void saveOrUpdateData(Map<String, String> base64String, String primarykey, AtomicInteger progressCount,
			String id, JSONObject jsonbody) {
		final int BATCH_SIZE = 500; // Define a batch size suitable for your needs
		String res = "";
		try {
			int total = base64String.size();
			int start = 0;

			while (start < total) {
				int end = Math.min(start + BATCH_SIZE, total);
				Map<String, String> batch = new HashMap<>(base64String).entrySet().stream().skip(start)
						.limit(BATCH_SIZE).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
//                System.err.println(batch);

				JSONObject savePDF = new JSONObject();
				savePDF.put("primary_id_pdf", primarykey);
				savePDF.put("document", batch);
//				savePDF.put("ids", jsonbody.get("ids").toString());
//				savePDF.put("byteofpdf", byteStream.toByteArray());
				progressCount.set(80);
				progress.put(id + "-" + primarykey, progressCount);
				socketService.pushSocketData(new JSONObject(), jsonbody, "progress");

				// First save
				LOGGER.warn("Enter into save");
				String url = pgresturl + "pdf_splitter";
				res = daHttpclientcaller.transmitDataspgrestpost(url, savePDF.toString(), false);
				if (Integer.parseInt(res) >= 200 && Integer.parseInt(res) <= 226) {
					progressCount.set(85);
					progress.put(id + "-" + primarykey, progressCount);
					socketService.pushSocketData(new JSONObject(), jsonbody, "progress");
				}
				LOGGER.warn(daHttpclientcaller.transmitDataspgrestpost(url, savePDF.toString(), false));

				start = end;
			}

		} catch (Exception e) {
			LOGGER.error("Error during save/update", e);
		}
	}

//
//	public boolean uploadFile(File file, String path) throws Exception {
////			File file = convertMultiPartFileToFile(mFile);
////			S3Upload s3Upload = new S3Upload();
//		if (s3Upload.NewuploadFile(ConfigurationFile.getStringConfig("s3bucket.bucketName").toString(), path, file,
//				true)) {
//			file.delete();
//			return true;
//		} else {
//			return false;
//		}
//	}
//
//	public void dara(MultipartFile file) {
//		File tempFile = null;
//		try {
//			// Save MultipartFile to a temporary file
//			tempFile = File.createTempFile("input", ".pdf");
//			file.transferTo(tempFile);
//
//			System.out.println("Temp file path: " + tempFile.getAbsolutePath());
//			System.out.println("File exists: " + tempFile.exists());
//
//			// Generate output file path
//			String bucketName = "goldenelement";
//			String outputFilePath = "output.png";
//			String s3FilePath = "refa/" + outputFilePath;
//
//			// Prepare commands
//			String[] convertCommand = { "convert", "-density", "300", tempFile.getAbsolutePath(), "output.png" };
//			String[] uploadCommand = { "aws", "s3", "cp", "output.png",
//					"https://goldenelement.s3.ap-southeast-1.amazonaws.com/" + bucketName + "/" + s3FilePath };
//
//			// Run the commands
//			runCommand(convertCommand);
//			runCommand(uploadCommand);
//
//			// Generate and print the S3 URL
//			String s3Url = generateS3Url(bucketName, s3FilePath);
//			System.out.println("File uploaded to S3. Accessible at: " + s3Url);
//
//		} catch (IOException | InterruptedException e) {
//			e.printStackTrace();
//		} finally {
//			// Clean up temporary file
//			if (tempFile != null && tempFile.exists()) {
//				tempFile.delete();
//			}
//		}
//	}
//
//	public static void runCommand(String[] command) throws IOException, InterruptedException {
//		ProcessBuilder processBuilder = new ProcessBuilder(command);
//		processBuilder.redirectErrorStream(true);
//
//		// Start the process
//		Process process = processBuilder.start();
//
//		// Capture output and error
//		try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
//			String line;
//			while ((line = reader.readLine()) != null) {
//				System.out.println(line); // Print output for debugging purposes
//			}
//		}
//
//		// Wait for the process to finish and check the exit code
//		int exitCode = process.waitFor();
//		if (exitCode != 0) {
//			// Print error message from stderr if available
//			try (BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
//				String errorLine;
//				while ((errorLine = errorReader.readLine()) != null) {
//					System.err.println(errorLine); // Print error for debugging purposes
//				}
//			}
//			throw new RuntimeException("Command failed with exit code: " + exitCode);
//		}
//	}
//
//	private String generateS3Url(String bucketName, String filePath) {
//		// Implement this method to generate the S3 URL
//		return "https://" + bucketName + ".s3.amazonaws.com/" + filePath;
//	}

}