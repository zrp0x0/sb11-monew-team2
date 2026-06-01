package com.codeit.monew.external.naver.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(NaverProperties.class)
public class NaverConfig {
}