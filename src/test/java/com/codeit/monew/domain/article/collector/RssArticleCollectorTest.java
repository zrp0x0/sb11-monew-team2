package com.codeit.monew.domain.article.collector;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.codeit.monew.domain.article.entity.Article;
import com.codeit.monew.domain.article.entity.ArticleSource;
import com.codeit.monew.domain.article.repository.ArticleRepository;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@ExtendWith(MockitoExtension.class)
class RssArticleCollectorTest {

    private static final String HANKYUNG_RSS_URL = "https://www.hankyung.com/feed/all-news";

    private RssProperties rssProperties;

    private RssArticleCollector rssArticleCollector;

    @BeforeEach
    void setUp() {
        rssProperties = new RssProperties();
        rssProperties.getHankyung().setUrl(HANKYUNG_RSS_URL);

        rssArticleCollector = new RssArticleCollector(
                articleRepository,
                restTemplateBuilder,
                rssProperties
        );
    }

    @Mock
    private ArticleRepository articleRepository;

    @Mock
    private RestTemplateBuilder restTemplateBuilder;

    @Mock
    private RestTemplate restTemplate;

    @Test
    @DisplayName("한국경제 RSS item을 Article로 저장한다")
    void collect_saveArticleFromRssItem() {
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
        when(articleRepository.existsBySourceUrlIncludingDeleted("https://www.hankyung.com/article/2026052974211"))
                .thenReturn(false);

        ArgumentCaptor<Article> articleCaptor = ArgumentCaptor.forClass(Article.class);

        // when
        RssArticleCollector.CollectResult result = rssArticleCollector.collect();

        // then
        assertThat(result.totalCount()).isEqualTo(1);
        assertThat(result.savedCount()).isEqualTo(1);
        assertThat(result.skippedCount()).isZero();
        assertThat(result.failedCount()).isZero();

        verify(articleRepository).save(articleCaptor.capture());

        Article savedArticle = articleCaptor.getValue();

        assertThat(savedArticle.getSource()).isEqualTo(ArticleSource.HANKYUNG);
        assertThat(savedArticle.getSourceUrl()).isEqualTo("https://www.hankyung.com/article/2026052974211");
        assertThat(savedArticle.getTitle()).isEqualTo("한국경제 테스트 기사");
        assertThat(savedArticle.getSummary()).isEqualTo("한국경제 테스트 기사 요약");
        assertThat(savedArticle.getPublishedAt()).isEqualTo(LocalDateTime.of(2026, 5, 29, 10, 0));
    }

    @Test
    @DisplayName("이미 같은 sourceUrl의 기사가 있으면 삭제 여부와 관계 없이 저장하지 않고 skip한다")
    void collect_skipDuplicatedSourceUrl() {
        // given
        String rssXml = """
                <rss>
                    <channel>
                        <item>
                            <title>중복 기사</title>
                            <link>https://www.hankyung.com/article/2026052974211</link>
                            <description>중복 기사 요약</description>
                            <pubDate>Fri, 29 May 2026 10:00:00 +0900</pubDate>
                        </item>
                    </channel>
                </rss>
                """;

        mockRssResponse(rssXml);
        when(articleRepository.existsBySourceUrlIncludingDeleted("https://www.hankyung.com/article/2026052974211"))
                .thenReturn(true);

        // when
        RssArticleCollector.CollectResult result = rssArticleCollector.collect();

        // then
        assertThat(result.totalCount()).isEqualTo(1);
        assertThat(result.savedCount()).isZero();
        assertThat(result.skippedCount()).isEqualTo(1);
        assertThat(result.failedCount()).isZero();

        verify(articleRepository, never()).save(org.mockito.ArgumentMatchers.any(Article.class));
    }

    @Test
    @DisplayName("필수 필드가 누락된 RSS item은 저장하지 않고 skip한다")
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
        RssArticleCollector.CollectResult result = rssArticleCollector.collect();

        // then
        assertThat(result.totalCount()).isEqualTo(1);
        assertThat(result.savedCount()).isZero();
        assertThat(result.skippedCount()).isEqualTo(1);
        assertThat(result.failedCount()).isZero();

        verify(articleRepository, never()).existsBySourceUrlIncludingDeleted(org.mockito.ArgumentMatchers.anyString());
        verify(articleRepository, never()).save(org.mockito.ArgumentMatchers.any(Article.class));
    }

    @Test
    @DisplayName("HTML 태그와 escape 문자를 정리하고 URL 추적 파라미터를 제거한다")
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
        when(articleRepository.existsBySourceUrlIncludingDeleted("https://www.hankyung.com/article/2026052974211"))
                .thenReturn(false);

        ArgumentCaptor<Article> articleCaptor = ArgumentCaptor.forClass(Article.class);

        // when
        RssArticleCollector.CollectResult result = rssArticleCollector.collect();

        // then
        assertThat(result.totalCount()).isEqualTo(1);
        assertThat(result.savedCount()).isEqualTo(1);
        assertThat(result.skippedCount()).isZero();
        assertThat(result.failedCount()).isZero();

        verify(articleRepository).save(articleCaptor.capture());

        Article savedArticle = articleCaptor.getValue();

        assertThat(savedArticle.getSourceUrl()).isEqualTo("https://www.hankyung.com/article/2026052974211");
        assertThat(savedArticle.getTitle()).isEqualTo("\"한국경제\" 테스트 기사");
        assertThat(savedArticle.getSummary()).isEqualTo("한국경제 테스트 기사 요약입니다.");
    }

    @Test
    @DisplayName("한국경제 RSS 호출에 실패하면 예외가 발생한다")
    void collect_throwExceptionWhenRssRequestFails() {
        // given
        when(restTemplateBuilder.build()).thenReturn(restTemplate);
        when(restTemplate.getForObject(eq(HANKYUNG_RSS_URL), eq(String.class)))
                .thenThrow(new RestClientException("RSS connection failed"));

        // when & then
        assertThatThrownBy(() -> rssArticleCollector.collect())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("한국경제 RSS 호출에 실패했습니다.");

        verify(articleRepository, never()).existsBySourceUrlIncludingDeleted(org.mockito.ArgumentMatchers.anyString());
        verify(articleRepository, never()).save(org.mockito.ArgumentMatchers.any(Article.class));
    }

    private void mockRssResponse(String rssXml) {
        when(restTemplateBuilder.build()).thenReturn(restTemplate);
        when(restTemplate.getForObject(eq(HANKYUNG_RSS_URL), eq(String.class)))
                .thenReturn(rssXml);
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
        when(articleRepository.existsBySourceUrlIncludingDeleted(
                "https://www.hankyung.com/article/2026052974211?keyword=%EC%82%BC%EC%84%B1%20AI"
        )).thenReturn(false);

        ArgumentCaptor<Article> articleCaptor = ArgumentCaptor.forClass(Article.class);

        // when
        RssArticleCollector.CollectResult result = rssArticleCollector.collect();

        // then
        assertThat(result.totalCount()).isEqualTo(1);
        assertThat(result.savedCount()).isEqualTo(1);
        assertThat(result.skippedCount()).isZero();
        assertThat(result.failedCount()).isZero();

        verify(articleRepository).save(articleCaptor.capture());

        Article savedArticle = articleCaptor.getValue();

        assertThat(savedArticle.getSourceUrl())
                .isEqualTo("https://www.hankyung.com/article/2026052974211?keyword=%EC%82%BC%EC%84%B1%20AI");
        assertThat(savedArticle.getSourceUrl()).doesNotContain("%25EC");
        assertThat(savedArticle.getSourceUrl()).doesNotContain("%2520");
    }
}

