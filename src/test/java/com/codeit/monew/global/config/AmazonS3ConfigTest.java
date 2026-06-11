package com.codeit.monew.global.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.amazonaws.services.s3.AmazonS3;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class AmazonS3ConfigTest {

  @Test
  @DisplayName("AWS 키가 모두 있으면 명시 자격 증명으로 S3 client를 생성")
  void createClientWithStaticCredentials() {
    AmazonS3Config config = config("access-key", "secret-key");

    AmazonS3 client = config.amazonS3Client();

    assertThat(client).isNotNull();
  }

  @Test
  @DisplayName("AWS 키가 없으면 기본 자격 증명 체인으로 S3 client를 생성")
  void createClientWithDefaultCredentialsProviderChain() {
    AmazonS3Config config = config("", "");

    AmazonS3 client = config.amazonS3Client();

    assertThat(client).isNotNull();
  }

  @Test
  @DisplayName("AWS 키가 일부만 있으면 예외 발생")
  void throwExceptionWhenOnlyOneCredentialExists() {
    AmazonS3Config config = config("access-key", "");

    assertThatThrownBy(config::amazonS3Client)
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("AWS access key and secret key must be configured together.");
  }

  private AmazonS3Config config(String accessKey, String secretKey) {
    AmazonS3Config config = new AmazonS3Config();
    ReflectionTestUtils.setField(config, "accessKey", accessKey);
    ReflectionTestUtils.setField(config, "secretKey", secretKey);
    ReflectionTestUtils.setField(config, "region", "ap-northeast-2");
    return config;
  }
}
