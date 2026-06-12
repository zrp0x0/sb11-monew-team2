package com.codeit.monew.global.config;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Configuration
public class AmazonS3Config {

  @Value("${cloud.aws.credentials.access-key:}")
  private String accessKey;

  @Value("${cloud.aws.credentials.secret-key:}")
  private String secretKey;

  @Value("${cloud.aws.region.static:ap-northeast-2}")
  private String region;

  @Bean
  public AmazonS3 amazonS3Client() {
    return AmazonS3ClientBuilder
        .standard()
        .withRegion(region)
        .withCredentials(credentialsProvider())
        .build();
  }

  private AWSCredentialsProvider credentialsProvider() {
    boolean hasAccessKey = StringUtils.hasText(accessKey);
    boolean hasSecretKey = StringUtils.hasText(secretKey);

    if (hasAccessKey && hasSecretKey) {
      BasicAWSCredentials credentials = new BasicAWSCredentials(accessKey, secretKey);
      return new AWSStaticCredentialsProvider(credentials);
    }

    if (hasAccessKey || hasSecretKey) {
      throw new IllegalStateException("AWS 액세스 키와 비밀 키는 함께 설정해야 합니다.");
    }

    return DefaultAWSCredentialsProviderChain.getInstance();
  }
}
