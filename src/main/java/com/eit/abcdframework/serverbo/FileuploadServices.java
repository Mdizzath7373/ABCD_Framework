package com.eit.abcdframework.serverbo;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
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

import javax.imageio.ImageIO;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
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

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.S3Object;
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

	@Autowired
	AmazonS3 amazonS3;

	@Autowired
	CommonServices commonServices;

	@Value("${applicationurl}")
	private String pgresturl;

	public static String s3url = ConfigurationFile.getStringConfig("s3bucket.url");

	private String path = ConfigurationFile.getStringConfig("s3bucket.path");

	private static Map<String, AtomicInteger> progress = new HashMap<>();

	public void setProgress(Map<String, AtomicInteger> progress) {
		FileuploadServices.progress = progress;
	}

	public Map<String, AtomicInteger> getProgress() {
		return progress;
	}

	private static final int maxRetries = 3;
	private static final int retryDelayMillis = 2000;

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
		return json;
	}

	public String convertPdfToMultipart(MultipartFile multipartFile, JSONObject gettabledata, JSONObject jsonbody)
			throws IOException {
		String Splitter_primary_id = gettabledata.getJSONObject("Splitter").getString("Splitter_primary_id");

		String primaryKey = jsonbody.get(Splitter_primary_id).toString();

		ExecutorService executorService = Executors.newFixedThreadPool(20);

		int pageCount = 0;
		Instant startTime = Instant.now();
		AtomicInteger progressCount = new AtomicInteger(0);
		final Map<Integer, String> base64String = new TreeMap<>();
		List<Integer> FaildPages = new ArrayList<>();

		try (InputStream inputStream = multipartFile.getInputStream();
				PDDocument document = PDDocument.load(inputStream)) {
			PDFRenderer pdfRenderer = new PDFRenderer(document);
			pageCount = document.getNumberOfPages();
			AtomicInteger preProgresCount = new AtomicInteger(0);
			List<Future<Boolean>> futures = new ArrayList<>();
			for (int pageIndex = 0; pageIndex < pageCount; pageIndex++) {
				int currentIndex = pageIndex;
				futures.add(executorService
						.submit(() -> processPage(document, pdfRenderer, currentIndex, base64String, primaryKey,
								progressCount, FaildPages, jsonbody.get("ids").toString(), jsonbody, preProgresCount)));
			}

			for (Future<Boolean> future : futures) {
				try {
					future.get();
				} catch (Exception e) {
					LOGGER.error(Thread.currentThread().getStackTrace()[0].getMethodName(),e);
				}
			}
		} finally {
			executorService.shutdown();
			try {
				if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
					executorService.shutdownNow();
				}
			} catch (InterruptedException ex) {
				executorService.shutdownNow();
				Thread.currentThread().interrupt();
				return "Failed";
			}
		}

		LOGGER.warn("ENTER INTO saveOrUpdateData {BASE64iMAGES}---------->" + Instant.now());
		saveOrUpdateData(base64String, primaryKey, progressCount, jsonbody.get("ids").toString(), jsonbody);
		LOGGER.warn("EXIT saveOrUpdateData {BASE64iMAGES}------->" + Instant.now());

		Instant endTime = Instant.now();
		Duration duration = Duration.between(startTime, endTime);
		LOGGER.warn(startTime + "_" + endTime + " Processing time: " + duration.toMillis() + " milliseconds");

		return "Success";
	}

	private boolean processPage(PDDocument document, PDFRenderer pdfRenderer, int pageIndex,
			Map<Integer, String> base64String, String primaryKey, AtomicInteger progressCount, List<Integer> FaildPages,
			String id, JSONObject jsonbody, AtomicInteger preProgresCount) {
		for (int attempt = 0; attempt <= maxRetries; attempt++) {
			try {

				synchronized (document) {
					BufferedImage bufferedImage = pdfRenderer.renderImageWithDPI(pageIndex, 150);
					ByteArrayOutputStream baos = new ByteArrayOutputStream();
					ImageIO.write(bufferedImage, "JPEG", baos);

					base64String.put((pageIndex + 1), "data:image/jpeg;base64,"
							+ Base64.getEncoder().encodeToString(baos.toByteArray()).replaceAll("=+$", ""));

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
					if (progressCount.get() > preProgresCount.get() || progressCount.get() == 75) {
						preProgresCount.set(progressCount.get());
						socketService.pushSocketData(new JSONObject(), jsonbody, "progress");
					}
				}
				LOGGER.warn("DONE page " + (pageIndex + 1));

				if ((pageIndex + 1) % 100 == 0) {
					LOGGER.warn("Sleeping after processing " + (pageIndex + 1) + " pages.");
					Thread.sleep(300000);
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
					return false;
				}
			} catch (Exception e) {
				FaildPages.add((pageIndex + 1));
				LOGGER.error("Error rendering page " + (pageIndex + 1), e);
				return false;
			}
		}
		return false;
	}

	private void saveOrUpdateData(Map<Integer, String> base64String, String primarykey, AtomicInteger progressCount,
			String id, JSONObject jsonbody) {
		String res = "";
		try {
			int total = base64String.size();
			JSONObject savePDF = new JSONObject();
			savePDF.put("primary_id_pdf", primarykey);
			savePDF.put("document", base64String);
			savePDF.put("total_pages", total);
			progressCount.set(80);
			progress.put(id + "-" + primarykey, progressCount);
			socketService.pushSocketData(new JSONObject(), jsonbody, "progress");

			LOGGER.warn("Enter into save");
			String url = pgresturl + "pdf_splitter";
			res = daHttpclientcaller.transmitDataspgrestpost(url, savePDF.toString(), false);
			if (Integer.parseInt(res) >= 200 && Integer.parseInt(res) <= 226) {
				progressCount.set(85);
				progress.put(id + "-" + primarykey, progressCount);
				socketService.pushSocketData(new JSONObject(), jsonbody, "progress");
			}
			LOGGER.warn(daHttpclientcaller.transmitDataspgrestpost(url, savePDF.toString(), false));

		} catch (Exception e) {
			LOGGER.error("Error during save/update", e);
		}
	}

	private void writePDF(PDDocument document, List<String> paths) {
		List<PDImageXObject> imagesList = new ArrayList<>();
		try {

			PDPage pages = new PDPage(PDRectangle.A4);
			document.addPage(pages);
			PDPageContentStream contentStream = new PDPageContentStream(document, pages);

			for (String imagePath : paths) {
				PDImageXObject images = PDImageXObject.createFromFile(imagePath, document);
				imagesList.add(images);
			}

			float width = pages.getMediaBox().getWidth();
			float height = pages.getMediaBox().getHeight();
			float currentY = height;

			for (PDImageXObject image : imagesList) {
				float imageWidth = image.getWidth();
				float imageHeight = image.getHeight();
				float scaleFactor = Math.min(width / imageWidth, height / imageHeight);
				float scaledWidth = imageWidth * scaleFactor;
				float scaledHeight = imageHeight * scaleFactor;

				currentY -= scaledHeight;

				if (currentY < 0) {
					contentStream.close();
					pages = new PDPage(PDRectangle.A4);
					document.addPage(pages);
					contentStream = new PDPageContentStream(document, pages);
					currentY = height - scaledHeight;
				}

				float x = (width - scaledWidth) / 2;
				contentStream.drawImage(image, x, currentY, scaledWidth, scaledHeight);

			}

			contentStream.close();

		} catch (Exception e) {
			LOGGER.error(Thread.currentThread().getStackTrace()[0].getMethodName(), e);
		}

	}

	public JSONObject writeImage(Map<String, Object> base64Images, String PDFpath, String filename, PDDocument document,
			MultipartFile file) {

		JSONObject res = new JSONObject();
		String currentDir = System.getProperty("user.dir");

		String nameofPDF = "split.pdf";

		String splitPDFPath = currentDir + nameofPDF;
		Map<String, String> s3paths = new HashMap<>();
		s3paths.put(nameofPDF.split("\\.")[0], splitPDFPath);

		if (!PDFpath.equalsIgnoreCase("")) {
			s3paths.put("original", PDFpath);
		}

		ExecutorService executorService = Executors.newFixedThreadPool(10);

		boolean IsFirst = true;

		if (IsFirst && !file.isEmpty()) {

			String paramImagePath = currentDir + "/MaterialPage." + file.getOriginalFilename().split("\\.")[1];
			File convFile = new File(paramImagePath);
			try {
				file.transferTo(convFile);

				S3Object s3Object = amazonS3.getObject("goldenelement", "download22.png");
				BufferedImage localImage = null;
				File S3file = null;
				try (InputStream inputStream = s3Object.getObjectContent();) {
					localImage = ImageIO.read(inputStream);
					String localpath = currentDir + "/S3Images.png";
					S3file = new File(localpath);

					ImageIO.write(localImage, "png", S3file);

					List<String> paths = new ArrayList<>();
					paths.add(paramImagePath);
					paths.add(localpath);
					writePDF(document, paths);
				} finally {
					if (S3file.exists()) {
						// Attempt to delete the file
						if (S3file.delete()) {
							LOGGER.info("Image deleted successfully.");
						} else {
							LOGGER.info("Failed to delete the image.");
						}
					} else {
						LOGGER.info("Image file does not exist.");
					}
					if (convFile.exists()) {
						// Attempt to delete the file
						if (convFile.delete()) {
							LOGGER.info("Image deleted successfully.");
						} else {
							LOGGER.info("Failed to delete the image.");
						}
					} else {
						LOGGER.info("Image file does not exist.");
					}
				}
			} catch (Exception e) {
				res.put("error", "Failed to split PDF, please retry.");
				LOGGER.error("Failed to process PDF", e);
			}

		}
		List<Future<String>> futures = new ArrayList<>();

		base64Images.entrySet().stream().sorted(Map.Entry.comparingByKey(Comparator.comparingInt(Integer::parseInt)))
				.forEach(entry -> {
					String imageName = entry.getKey();
					String base64Image = (String) entry.getValue();
					byte[] imageBytes = Base64.getDecoder().decode(base64Image.replace("data:image/jpeg;base64,", ""));
					futures.add(executorService.submit(() -> {
						File outputfile = null;
						try (ByteArrayInputStream bis = new ByteArrayInputStream(imageBytes)) {
							BufferedImage bufferedImage = ImageIO.read(bis);
							String imagePath = currentDir + imageName + ".jpeg";
							outputfile = new File(imagePath);
							ImageIO.write(bufferedImage, "JPEG", outputfile);
							return imagePath;
						} catch (IOException e) {
							LOGGER.error(Thread.currentThread().getStackTrace()[0].getMethodName(), e);
							return null;
						}
					}));
				});

		try {
			List<String> imagePaths = new ArrayList<>();
			for (Future<String> future : futures) {
				String imagePath = future.get();
				if (imagePath != null) {
					imagePaths.add(imagePath);
				}
			}

			imagePaths.stream().forEach(entryData -> {
				File removeImge = null;
				try {
					List<String> path = new ArrayList<String>();
					path.add(entryData);
					removeImge = new File(entryData);
					writePDF(document, path);
				} finally {
					if (removeImge != null && removeImge.exists()) {
						if (removeImge.delete()) {
							LOGGER.info("Image deleted successfully.");
						} else {
							LOGGER.info("Failed to delete the image.");
						}
					}
				}
			});

			document.save(splitPDFPath);

			if (!file.isEmpty() && !PDFpath.equalsIgnoreCase("")) {
				document.removePage(0);
				document.save(PDFpath);
			} else if (file.isEmpty() && PDFpath.equalsIgnoreCase("")) {
				document.save(PDFpath);
			} else {
				LOGGER.info("Original PDF Not saved!");
			}
			s3paths.forEach((key, value) -> {
				String filePath = path + filename + key + dateFormat.format(new Date()) + ".pdf";
				try {
					if (PDFUploadS3(value, filePath)) {
						res.put(key, s3url + filePath);
					} else {
						res.put("error", "Failed to save to S3 bucket.");
					}
				} catch (Exception e) {
					LOGGER.error("Failed to upload to S3", e);
					res.put("error", "Failed to save to S3 bucket.");
				}
			});
		} catch (Exception e) {
			res.put("error", "Failed to split PDF, please retry.");
			LOGGER.error("Failed to process PDF", e);
		} finally {
			executorService.shutdown();
		}

		return res;
	}

	private boolean PDFUploadS3(String PDFpath, String filePath) throws Exception {
		File file = new File(PDFpath);
		if (s3Upload.NewuploadFile(ConfigurationFile.getStringConfig("s3bucket.bucketName").toString(), filePath, file,
				true)) {
			file.delete();
			return true;
		} else {
			return false;
		}
	}

	public int mergebase64ToPDF(JSONObject gettabledata, JSONObject jsonbody, MultipartFile files) {
		int primary_id = 0;
		try {

			JSONArray jarr = new JSONArray(gettabledata.getJSONArray("filepathname").toString());
			JSONArray column = new JSONArray(gettabledata.getJSONArray("column").toString());
			String filename = "";

			String Splitter_primary_id = gettabledata.getJSONObject("Splitter").getString("Splitter_primary_id");

			String value = jsonbody.get(Splitter_primary_id).toString();

			for (int i = 0; i < jarr.length(); i++) {
				filename += jsonbody.get(jarr.get(i).toString());
			}
			String currentDir = System.getProperty("user.dir");
			String PDFpath = "";

			if (gettabledata.getJSONObject("Splitter").has("original")
					&& gettabledata.getJSONObject("Splitter").getBoolean("original")) {
				PDFpath = currentDir + "original.pdf";
			}
			String geturl = pgresturl + "pdf_splitter?select=total_pages,id&primary_id_pdf=eq." + value;
			JSONObject datavalue = new JSONObject(daHttpclientcaller.transmitDataspgrest(geturl).get(0).toString());
			int total_pages = datavalue.getInt("total_pages");
			primary_id = datavalue.getInt("id");

			Map<String, Object> base64Images = commonServices.loadBase64(value, total_pages);
			JSONObject path = null;
			try (PDDocument document = new PDDocument()) {
				path = writeImage(base64Images, PDFpath, filename, document, files);
				if (path.has("error")) {
					return 0;
				}
				jsonbody.put(column.get(0).toString(), path);
			}
		} catch (Exception e) {
			LOGGER.error(Thread.currentThread().getStackTrace()[0].getMethodName(), e);
		}
		return primary_id;
	}

}