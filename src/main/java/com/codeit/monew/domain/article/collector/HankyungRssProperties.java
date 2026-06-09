package com.codeit.monew.domain.article.collector;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "external-api.hankyung")
public class HankyungRssProperties {

    private String rssUrl;
}