package com.eit.abcdframework.s3bucket;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.SdkClientException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AccessControlList;
import com.amazonaws.services.s3.model.CompleteMultipartUploadRequest;
import com.amazonaws.services.s3.model.GroupGrantee;
import com.amazonaws.services.s3.model.InitiateMultipartUploadRequest;
import com.amazonaws.services.s3.model.InitiateMultipartUploadResult;
import com.amazonaws.services.s3.model.PartETag;
import com.amazonaws.services.s3.model.Permission;
import com.amazonaws.services.s3.model.UploadPartRequest;
import com.eit.abcdframework.config.ConfigurationFile;

//@Component
public class S3Service {
	private static final Logger LOGGER = LoggerFactory.getLogger(S3Service.class);

	@Autowired
	private AmazonS3 s3Client;

	public static String AMAZON_AWS = ConfigurationFile.getStringConfig("s3bucket.url"); // "https://kingstrackimages.s3.amazonaws.com/";

	public boolean uploadFile(String bucketName, String key, MultipartFile file, boolean isPublic) {
		boolean status = true;
		try {
			List<File> parts = splitFile(file);

			InitiateMultipartUploadRequest initRequest = new InitiateMultipartUploadRequest(bucketName, key);
			InitiateMultipartUploadResult initResponse = s3Client.initiateMultipartUpload(initRequest);
			String uploadId = initResponse.getUploadId();

			List<PartETag> partETags = new ArrayList<>();
			for (int i = 0; i < parts.size(); i++) {
				File partFile = parts.get(i);
				UploadPartRequest uploadRequest = new UploadPartRequest().withBucketName(bucketName).withKey(key)
						.withUploadId(uploadId).withPartNumber(i + 1).withFile(partFile)
						.withPartSize(partFile.length());

				partETags.add(s3Client.uploadPart(uploadRequest).getPartETag());

			}
			CompleteMultipartUploadRequest completeRequest = new CompleteMultipartUploadRequest(bucketName, key,
					uploadId, partETags);
			s3Client.completeMultipartUpload(completeRequest);
			s3Client.getObject(bucketName, key).getObjectContent();
			if (true) {
				AccessControlList acl = s3Client.getObjectAcl(bucketName, key);
				acl.grantPermission(GroupGrantee.AllUsers, Permission.Read);
				s3Client.setObjectAcl(bucketName, key, acl);
			}
			LOGGER.info("View public object contents here:" + AMAZON_AWS + key);

			// Clean up
			for (File part : parts) {
				part.delete();
			}

			return status;
		} catch (AmazonServiceException e) {
			status = false; // Handle Amazon service exception
			LOGGER.error("Caught an AmazonServiceException: " + e.getMessage());
		} catch (SdkClientException e) {
			status = false; // Handle SDK client exception
			LOGGER.error("Caught an AmazonServiceException: " + e.getMessage());
		} catch (IOException e) {
			status = false; // Handle IO exception
			LOGGER.error("Caught an AmazonServiceException: " + e.getMessage());
		}
		return status;
	}

	private List<File> splitFile(MultipartFile file) throws IOException {
		List<File> parts = new ArrayList<>();
		int partCounter = 1;
		long fileSize = file.getSize();
		int sizeOfFiles = 1024 * 1024 * 5; // 5MB
		byte[] buffer = new byte[sizeOfFiles];

		try (BufferedInputStream bis = new BufferedInputStream(file.getInputStream())) {
			String fileName = file.getOriginalFilename();
			int tmp = 0;
			while ((tmp = bis.read(buffer)) > 0) {
				File newFile = new File(fileName + ".part" + partCounter);
				try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(newFile))) {
					bos.write(buffer, 0, tmp);
				}
				parts.add(newFile);
				partCounter++;
			}
		}

		return parts;
	}
}
