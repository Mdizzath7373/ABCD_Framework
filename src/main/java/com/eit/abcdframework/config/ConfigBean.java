package com.eit.abcdframework.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;

@Configuration
public class ConfigBean {

	@Bean
	public BasicAWSCredentials basicAWSCredentials() {
		String ACCESS_KEY = ConfigurationFile.getStringConfig("s3bucket.accessKey");
		String SECRET_KEY = ConfigurationFile.getStringConfig("s3bucket.secretKey");
		return new BasicAWSCredentials(ACCESS_KEY, SECRET_KEY);
	}

	@Bean
	public AmazonS3 amazonS3() {
		return AmazonS3ClientBuilder.standard().withRegion("ap-southeast-1")
				.withCredentials(new AWSStaticCredentialsProvider(basicAWSCredentials())).build();
	}
}