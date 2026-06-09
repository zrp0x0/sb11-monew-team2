package com.codeit.monew.domain.article.collector;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.codeit.monew.domain.article.entity.ArticleSource;
import java.time.LocalDateTime;
import java.util.List;
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
class RssNewsProviderTest {

    private static final String HANKYUNG_RSS_URL = "https://www.hankyung.com/feed/all-news";

    @Mock
    private RestTemplateBuilder restTemplateBuilder;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private HankyungRssProperties hankyungRssProperties;

    @InjectMocks
    private RssNewsProvider rssNewsProvider;

    @Test
    @DisplayName("한국경제 RSS item을 CollectedArticle로 변환")
    void collect_returnCollectedArticlesFromRssItem() {
        // given
        String rssXml = """
                <rss>
                    <channel>
                        <item>
                            <title>한국경제 테스트 기사</title>
                            <link>https://www.hankyung.com/article/2026052974211</link>
                            <description>한국경제 테스트 기사 요약</description>
                            <pubDate>Fri, 29 May 2026 10:00:00 +0900</pubDate>
                        </item>
                    </channel>
                </rss>
                """;

        mockRssResponse(rssXml);

        // when
        List<CollectedArticle> result = rssNewsProvider.collect();

        // then
        assertThat(result).hasSize(1);

        CollectedArticle article = result.get(0);

        assertThat(article.source()).isEqualTo(ArticleSource.HANKYUNG);
        assertThat(article.sourceUrl()).isEqualTo("https://www.hankyung.com/article/2026052974211");
        assertThat(article.title()).isEqualTo("한국경제 테스트 기사");
        assertThat(article.summary()).isEqualTo("한국경제 테스트 기사 요약");
        assertThat(article.publishedAt()).isEqualTo(LocalDateTime.of(2026, 5, 29, 10, 0));
    }

    @Test
    @DisplayName("필수 필드가 누락된 RSS item은 수집 결과에서 제외")
    void collect_skipInvalidRssItem() {
        // given
        String rssXml = """
                <rss>
                    <channel>
                        <item>
                            <title>요약 없는 기사</title>
                            <link>https://www.hankyung.com/article/2026052974211</link>
                            <pubDate>Fri, 29 May 2026 10:00:00 +0900</pubDate>
                        </item>
                    </channel>
                </rss>
                """;

        mockRssResponse(rssXml);

        // when
        List<CollectedArticle> result = rssNewsProvider.collect();

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("HTML 태그와 escape 문자를 정리하고 URL 추적 파라미터 제거")
    void collect_cleanHtmlAndNormalizeUrl() {
        // given
        String rssXml = """
                <rss>
                    <channel>
                        <item>
                            <title>&quot;한국경제&quot; 테스트 기사</title>
                            <link>https://www.hankyung.com/article/2026052974211/?utm_source=naver&amp;utm_medium=referral#comment</link>
                            <description><![CDATA[<p>한국경제 <b>테스트</b> 기사 요약입니다.</p>]]></description>
                            <pubDate>Fri, 29 May 2026 10:00:00 +0900</pubDate>
                        </item>
                    </channel>
                </rss>
                """;

        mockRssResponse(rssXml);

        // when
        List<CollectedArticle> result = rssNewsProvider.collect();

        // then
        assertThat(result).hasSize(1);

        CollectedArticle article = result.get(0);

        assertThat(article.sourceUrl()).isEqualTo("https://www.hankyung.com/article/2026052974211");
        assertThat(article.title()).isEqualTo("\"한국경제\" 테스트 기사");
        assertThat(article.summary()).isEqualTo("한국경제 테스트 기사 요약입니다.");
    }

    @Test
    @DisplayName("이미 인코딩된 query parameter를 이중 인코딩하지 않는다")
    void collect_doesNotDoubleEncodeRawQueryParameter() {
        // given
        String rssXml = """
                <rss>
                    <channel>
                        <item>
                            <title>인코딩 테스트 기사</title>
                            <link>https://www.hankyung.com/article/2026052974211?keyword=%EC%82%BC%EC%84%B1%20AI&amp;utm_source=naver</link>
                            <description>인코딩 테스트 기사 요약</description>
                            <pubDate>Fri, 29 May 2026 10:00:00 +0900</pubDate>
                        </item>
                    </channel>
                </rss>
                """;

        mockRssResponse(rssXml);

        // when
        List<CollectedArticle> result = rssNewsProvider.collect();

        // then
        assertThat(result).hasSize(1);

        CollectedArticle article = result.get(0);

        assertThat(article.sourceUrl())
                .isEqualTo("https://www.hankyung.com/article/2026052974211?keyword=%EC%82%BC%EC%84%B1%20AI");
        assertThat(article.sourceUrl()).doesNotContain("%25EC");
        assertThat(article.sourceUrl()).doesNotContain("%2520");
    }

    @Test
    @DisplayName("한국경제 RSS 호출에 실패하면 예외 발생")
    void collect_throwExceptionWhenRssRequestFails() {
        // given
        when(hankyungRssProperties.getRssUrl()).thenReturn(HANKYUNG_RSS_URL);
        when(restTemplateBuilder.build()).thenReturn(restTemplate);
        when(restTemplate.getForObject(eq(HANKYUNG_RSS_URL), eq(String.class)))
                .thenThrow(new RestClientException("RSS connection failed"));

        // when & then
        assertThatThrownBy(() -> rssNewsProvider.collect())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("한국경제 RSS 호출에 실패했습니다.");
    }

    @Test
    @DisplayName("한국경제 RSS URL 설정이 없으면 예외 발생")
    void collect_throwExceptionWhenRssUrlIsBlank() {
        // given
        when(hankyungRssProperties.getRssUrl()).thenReturn("");

        // when & then
        assertThatThrownBy(() -> rssNewsProvider.collect())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("한국경제 RSS URL 설정이 필요합니다.");
    }

    private void mockRssResponse(String rssXml) {
        when(hankyungRssProperties.getRssUrl()).thenReturn(HANKYUNG_RSS_URL);
        when(restTemplateBuilder.build()).thenReturn(restTemplate);
        when(restTemplate.getForObject(eq(HANKYUNG_RSS_URL), eq(String.class)))
                .thenReturn(rssXml);
    }
}