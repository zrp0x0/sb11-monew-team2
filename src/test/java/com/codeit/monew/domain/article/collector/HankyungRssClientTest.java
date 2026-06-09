package com.codeit.monew.domain.article.collector;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@ExtendWith(MockitoExtension.class)
class HankyungRssClientTest {

    private static final String HANKYUNG_RSS_URL = "https://www.hankyung.com/feed/all-news";

    @Mock
    private RestTemplateBuilder restTemplateBuilder;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private HankyungRssProperties hankyungRssProperties;

    @InjectMocks
    private HankyungRssClient hankyungRssClient;

    @Test
    @DisplayName("한국경제 RSS XML 호출")
    void fetchRssXml_returnRssXml() {
        // given
        String rssXml = "<rss><channel></channel></rss>";

        when(hankyungRssProperties.getRssUrl()).thenReturn(HANKYUNG_RSS_URL);
        when(restTemplateBuilder.build()).thenReturn(restTemplate);
        when(restTemplate.getForObject(eq(HANKYUNG_RSS_URL), eq(String.class)))
                .thenReturn(rssXml);

        // when
        String result = hankyungRssClient.fetchRssXml();

        // then
        assertThat(result).isEqualTo(rssXml);
    }

    @Test
    @DisplayName("한국경제 RSS URL 설정이 없으면 예외 발생")
    void fetchRssXml_throwExceptionWhenRssUrlIsBlank() {
        // given
        when(hankyungRssProperties.getRssUrl()).thenReturn("");

        // when & then
        assertThatThrownBy(() -> hankyungRssClient.fetchRssXml())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("한국경제 RSS URL 설정이 필요합니다.");
    }

    @Test
    @DisplayName("한국경제 RSS 응답이 비어 있으면 예외 발생")
    void fetchRssXml_throwExceptionWhenRssResponseIsBlank() {
        // given
        when(hankyungRssProperties.getRssUrl()).thenReturn(HANKYUNG_RSS_URL);
        when(restTemplateBuilder.build()).thenReturn(restTemplate);
        when(restTemplate.getForObject(eq(HANKYUNG_RSS_URL), eq(String.class)))
                .thenReturn("");

        // when & then
        assertThatThrownBy(() -> hankyungRssClient.fetchRssXml())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("한국경제 RSS 응답이 비어 있습니다.");
    }

    @Test
    @DisplayName("한국경제 RSS 호출에 실패하면 예외 발생")
    void fetchRssXml_throwExceptionWhenRequestFails() {
        // given
        when(hankyungRssProperties.getRssUrl()).thenReturn(HANKYUNG_RSS_URL);
        when(restTemplateBuilder.build()).thenReturn(restTemplate);
        when(restTemplate.getForObject(eq(HANKYUNG_RSS_URL), eq(String.class)))
                .thenThrow(new RestClientException("RSS connection failed"));

        // when & then
        assertThatThrownBy(() -> hankyungRssClient.fetchRssXml())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("한국경제 RSS 호출에 실패했습니다.");
    }
}