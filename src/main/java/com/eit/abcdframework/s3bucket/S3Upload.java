
/*
 * S3Upload.java
 *
 * Copyright notice
 */

package com.eit.abcdframework.s3bucket;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.SdkClientException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AccessControlList;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.GroupGrantee;
import com.amazonaws.services.s3.model.Permission;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.TransferManagerBuilder;
import com.amazonaws.services.s3.transfer.Upload;
import com.eit.abcdframework.config.ConfigurationFile;

/*
 * S3Upload class file is used to store the files in the Amazon S3 (Simple Storage Service)
 * repository.This class can be used to upload, read and delete the files from the S3
 * repository.
 * 
 * @author Fazel
 * @version 1.0
 * @Date 03, July 2010
 */

@Component
public class S3Upload {
	private static final Logger LOGGER = LoggerFactory.getLogger(S3Upload.class);

	public static String AMAZON_AWS = ConfigurationFile.getStringConfig("s3bucket.url"); // "https://kingstrackimages.s3.amazonaws.com/";
	@Autowired
	AmazonS3 s3 ;

	/*
	 * Retrieves the access credentials for the AWS repository from the properties
	 * file and create new AmazonS3Client object.
	 */
//	BasicAWSCredentials awsCredentials = new BasicAWSCredentials(ACCESS_KEY, SECRET_KEY);
//
//	public S3Upload() throws IOException {
//
////		s3 = new AmazonS3Client(new PropertiesCredentials(
////				S3Upload.class.getResourceAsStream("/AwsCredentials_" + buckName + ".properties")));
////		s3 = new AmazonS3Client(awsCredentials);
//		s3 = AmazonS3ClientBuilder.standard().withCredentials(new AWSStaticCredentialsProvider(awsCredentials))
//				.withRegion(Regions.US_WEST_1).build();
////		s3 = AmazonS3ClientBuilder.standard().withCredentials(new AWSStaticCredentialsProvider(awsCredentials)).build();
//	}

	/*
	 * To upload a file into the S3 repository and give the permissions to the file
	 * based on the parameters passed.
	 * 
	 * @param bucketName Name of the bucket in the S3 repository
	 * 
	 * @param key Unique identifier in the bucket
	 * 
	 * @param file Fully qualified path and the file name
	 * 
	 * @param isPublic Boolean value to mention whether the file should be given
	 * public access
	 */

	public boolean akuploadFile(String bucketName, String key, File file, boolean isPublic) throws IOException {

		boolean status = true;

		try {
			s3.putObject(new PutObjectRequest(bucketName, key, file));

			if (isPublic) {
				AccessControlList acl = s3.getObjectAcl(bucketName, key);
				acl.grantPermission(GroupGrantee.AllUsers, Permission.Read);
				s3.setObjectAcl(bucketName, key, acl);
			}

			LOGGER.info("View public object contents here:" + AMAZON_AWS + key);

		} catch (AmazonServiceException ase) {
			status = false;
			LOGGER.error("Caught an AmazonServiceException: " + ase.getMessage());
		} catch (AmazonClientException ace) {
			status = false;
			LOGGER.error("Caught an AmazonClientException: " + ace.getMessage());
		}
		return status;
	}

	public boolean uploadFile(String bucketName, String key, File file, boolean isPublic) throws IOException {

		boolean status = true;

		try {

			key = key + file.getName();

			//

			s3.putObject(new PutObjectRequest(bucketName, key, file));

			if (isPublic) {
				AccessControlList acl = s3.getObjectAcl(bucketName, key);
				acl.grantPermission(GroupGrantee.AllUsers, Permission.Read);
				s3.setObjectAcl(bucketName, key, acl);
			}

			// System.out.println("View public object contents here:"
			// + AMAZON_AWS + bucketName + "/" + key);

			System.out.println("View public object contents here:" + AMAZON_AWS + key);

		} catch (AmazonServiceException ase) {
			status = false;
			System.out.println("Caught an AmazonServiceException, which means your request made it "
					+ "to Amazon S3, but was rejected with an error response for some reason.");

		} catch (AmazonClientException ace) {
			status = false;
			System.out.println("Caught an AmazonClientException, which means the client encountered "
					+ "a serious internal problem while trying to communicate with S3, "
					+ "such as not being able to access the network.");

		}
		return status;
	}

	public boolean NewuploadFile(String bucketName, String key, File file, boolean isPublic)
			throws AmazonClientException, InterruptedException {
//		Regions clientRegion = Regions.DEFAULT_REGION;
		boolean status = true;
		try {
			
//			AmazonS3 s3Client = AmazonS3ClientBuilder.standard().withRegion(clientRegion)
//					.withCredentials(new ProfileCredentialsProvider()).build();
			TransferManager tm = TransferManagerBuilder.standard().withS3Client(s3).build();

			// TransferManager processes all transfers asynchronously,
			// so this call returns immediately.
			Upload upload = tm.upload(bucketName, key, file);
			LOGGER.info("Object upload started");

			// Optionally, wait for the upload to finish before continuing.
			upload.waitForCompletion();
			LOGGER.info("Permission of S3Bucket:::");
			s3.setObjectAcl(bucketName, key, CannedAccessControlList.PublicRead);
			LOGGER.info("Object upload complete");
		} catch (AmazonServiceException e) {
			// The call was transmitted successfully, but Amazon S3 couldn't process
			// it, so it returned an error response.
			status = false;
			LOGGER.error("Caught an AmazonServiceException: " + e.getMessage());

		} catch (SdkClientException e) {
			// Amazon S3 couldn't be contacted for a response, or the client
			// couldn't parse the response from Amazon S3.
			status = false;
			LOGGER.error("Caught an AmazonServiceException: " + e.getMessage());

		}
		return status;
	}
	/*
	 * To read a file from the S3 repository
	 * 
	 * @param bucketName Name of the bucket in the S3 repository
	 * 
	 * @param key Unique identifier in the bucket
	 */

	public InputStream readFile(String bucketName, String key) throws IOException {
		S3Object object = null;
		try {

			object = s3.getObject(new GetObjectRequest(bucketName, key));
			System.out.println("Content-Type: " + object.getObjectMetadata().getContentType());
			return object.getObjectContent();
		} catch (AmazonServiceException ase) {
			System.out.println("Caught an AmazonServiceException, which means your request made it "
					+ "to Amazon S3, but was rejected with an error response for some reason.");

		} catch (AmazonClientException ace) {
			System.out.println("Caught an AmazonClientException, which means the client encountered "
					+ "a serious internal problem while trying to communicate with S3, "
					+ "such as not being able to access the network.");

		}
		return object.getObjectContent();
	}

	/*
	 * To delete a file in the S3 repository
	 * 
	 * 
	 * @param bucketName Name of the bucket in the S3 repository
	 * 
	 * @param key Unique identifier in the bucket
	 */

	public boolean deleteFile(String bucketName, String key) throws IOException {
		boolean status = true;
		try {
			s3.deleteObject(bucketName, key);

		} catch (AmazonServiceException ase) {
			status = false;
			System.out.println("Caught an AmazonServiceException, which means your request made it "
					+ "to Amazon S3, but was rejected with an error response for some reason.");

		} catch (AmazonClientException ace) {
			status = false;
			System.out.println("Caught an AmazonClientException, which means the client encountered "
					+ "a serious internal problem while trying to communicate with S3, "
					+ "such as not being able to access the network.");

		}
		return status;
	}

	/*
	 * To read the file from the S3 repository
	 * 
	 * @param input file content passed as InputStream
	 */
	private static void displayTextInputStream(InputStream input) throws IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(input));
		while (true) {
			String line = reader.readLine();
			if (line == null)
				break;

		}

	}
	
	private void uploadFolderToS3(File folder,String bucketName) {
        if (folder.isDirectory()) {
            File[] files = folder.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile()) {
                        try {
                            PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, file.getName(), file);
                            s3.putObject(putObjectRequest);
                            System.out.println("Uploaded file: " + file.getName());
                        } catch (Exception e) {
                            System.err.println("Error uploading file to S3: " + file.getName());
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    }

	/*
	 * To check whether the file exists in the S3 repository
	 * 
	 * @param bucketName Name of the bucket in the S3 repository
	 * 
	 * @param key Unique identifier in the bucket
	 */
	public boolean fileExist(String bucketName, String key) throws IOException {
		boolean fileExist = true;

		try {

			S3Object obj = s3.getObject(bucketName, key);

		} catch (AmazonServiceException se) {
			fileExist = false;

		}
		return fileExist;
	}
}