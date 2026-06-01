package com.codeit.monew.external.naver.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "naver")
public class NaverProperties {

    private String clientId;
    private String clientSecret;
}