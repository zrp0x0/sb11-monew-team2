package com.codeit.monew.global.config;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AmazonS3Config {

  @Bean
  public AmazonS3 amazonS3Client() {
    // TODO: 아직 키가 없으므로 테스틀를 위해 아무 문자열이나 넣음
    BasicAWSCredentials credentials = new BasicAWSCredentials("dummy-access-key", "dummy-secret-key");

    return AmazonS3ClientBuilder
        .standard()
        .withRegion("ap-northeast-2")
        .withCredentials(new AWSStaticCredentialsProvider(credentials))
        .build();
  }
}
