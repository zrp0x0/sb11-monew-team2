package com.codeit.monew.infra.externalapi.hankyung.client;

import com.codeit.monew.infra.externalapi.hankyung.properties.HankyungRssProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
public class HankyungRssClient {

    private final RestTemplateBuilder restTemplateBuilder;
    private final HankyungRssProperties hankyungRssProperties;

    public String fetchRssXml() {
        String rssUrl = hankyungRssProperties.getRssUrl();

        if (!StringUtils.hasText(rssUrl)) {
            throw new IllegalStateException("한국경제 RSS URL 설정이 필요합니다.");
        }

        try {
            RestTemplate restTemplate = restTemplateBuilder.build();
            String rssXml = restTemplate.getForObject(rssUrl, String.class);

            if (!StringUtils.hasText(rssXml)) {
                throw new IllegalStateException("한국경제 RSS 응답이 비어 있습니다.");
            }

            return rssXml;
        } catch (RestClientException e) {
            throw new IllegalStateException("한국경제 RSS 호출에 실패했습니다.", e);
        }
    }
}