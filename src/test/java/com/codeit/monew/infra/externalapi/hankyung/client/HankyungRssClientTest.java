package com.codeit.monew.infra.externalapi.hankyung.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.codeit.monew.infra.externalapi.hankyung.properties.HankyungRssProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

@ExtendWith(MockitoExtension.class)
class HankyungRssClientTest {

    @Mock
    private RestTemplateBuilder restTemplateBuilder;

    @Test
    @DisplayName("설정된 RSS URL에서 XML 문자열을 가져온다")
    void fetchRssXml_ValidResponse_ReturnsXml() {
        // given
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        when(restTemplateBuilder.build()).thenReturn(restTemplate);

        HankyungRssProperties properties = new HankyungRssProperties();
        properties.setRssUrl("https://www.hankyung.com/feed/all-news");

        server.expect(requestTo("https://www.hankyung.com/feed/all-news"))
                .andRespond(withSuccess("<rss><channel></channel></rss>", MediaType.APPLICATION_XML));

        HankyungRssClient client = new HankyungRssClient(restTemplateBuilder, properties);

        // when
        String result = client.fetchRssXml();

        // then
        assertThat(result).isEqualTo("<rss><channel></channel></rss>");
        server.verify();
    }

    @Test
    @DisplayName("RSS URL 설정이 없으면 예외를 던진다")
    void fetchRssXml_BlankUrl_ThrowsException() {
        // given
        HankyungRssProperties properties = new HankyungRssProperties();
        properties.setRssUrl(" ");
        HankyungRssClient client = new HankyungRssClient(restTemplateBuilder, properties);

        // when & then
        assertThatThrownBy(client::fetchRssXml)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("한국경제 RSS URL 설정이 필요합니다.");
    }

    @Test
    @DisplayName("RSS 응답 본문이 비어 있으면 예외를 던진다")
    void fetchRssXml_BlankResponse_ThrowsException() {
        // given
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        when(restTemplateBuilder.build()).thenReturn(restTemplate);

        HankyungRssProperties properties = new HankyungRssProperties();
        properties.setRssUrl("https://www.hankyung.com/feed/all-news");

        server.expect(requestTo("https://www.hankyung.com/feed/all-news"))
                .andRespond(withSuccess(" ", MediaType.APPLICATION_XML));

        HankyungRssClient client = new HankyungRssClient(restTemplateBuilder, properties);

        // when & then
        assertThatThrownBy(client::fetchRssXml)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("한국경제 RSS 응답이 비어 있습니다.");
        server.verify();
    }

    @Test
    @DisplayName("RSS 호출 실패는 IllegalStateException으로 변환한다")
    void fetchRssXml_RestClientException_ThrowsException() {
        // given
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        when(restTemplateBuilder.build()).thenReturn(restTemplate);

        HankyungRssProperties properties = new HankyungRssProperties();
        properties.setRssUrl("https://www.hankyung.com/feed/all-news");

        server.expect(requestTo("https://www.hankyung.com/feed/all-news"))
                .andRespond(withServerError());

        HankyungRssClient client = new HankyungRssClient(restTemplateBuilder, properties);

        // when & then
        assertThatThrownBy(client::fetchRssXml)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("한국경제 RSS 호출에 실패했습니다.");
        server.verify();
    }
}
