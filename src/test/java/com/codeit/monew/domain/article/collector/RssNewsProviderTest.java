package com.codeit.monew.domain.article.collector;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.codeit.monew.domain.article.collector.HankyungRssParser.RssItem;
import com.codeit.monew.domain.article.entity.ArticleSource;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RssNewsProviderTest {

    @Mock
    private HankyungRssClient hankyungRssClient;

    @Mock
    private HankyungRssParser hankyungRssParser;

    @InjectMocks
    private RssNewsProvider rssNewsProvider;

    @Test
    @DisplayName("한국경제 RSS item을 CollectedArticle로 변환")
    void collect_returnCollectedArticlesFromRssItem() {
        // given
        String rssXml = "<rss></rss>";

        when(hankyungRssClient.fetchRssXml()).thenReturn(rssXml);
        when(hankyungRssParser.parse(rssXml)).thenReturn(List.of(
                new RssItem(
                        "한국경제 테스트 기사",
                        "https://www.hankyung.com/article/2026052974211",
                        "한국경제 테스트 기사 요약",
                        "Fri, 29 May 2026 10:00:00 +0900"
                )
        ));

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
        String rssXml = "<rss></rss>";

        when(hankyungRssClient.fetchRssXml()).thenReturn(rssXml);
        when(hankyungRssParser.parse(rssXml)).thenReturn(List.of(
                new RssItem(
                        "링크 없는 기사",
                        null,
                        "링크 없는 기사 요약",
                        "Tue, 09 Jun 2026 16:01:30 +0900"
                )
        ));

        // when
        List<CollectedArticle> result = rssNewsProvider.collect();

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("HTML 태그와 escape 문자를 정리하고 URL 추적 파라미터 제거")
    void collect_cleanHtmlAndNormalizeUrl() {
        // given
        String rssXml = "<rss></rss>";

        when(hankyungRssClient.fetchRssXml()).thenReturn(rssXml);
        when(hankyungRssParser.parse(rssXml)).thenReturn(List.of(
                new RssItem(
                        "&quot;한국경제&quot; 테스트 기사",
                        "https://www.hankyung.com/article/2026052974211/?utm_source=naver&utm_medium=referral#comment",
                        "<p>한국경제 <b>테스트</b> 기사 요약입니다.</p>",
                        "Fri, 29 May 2026 10:00:00 +0900"
                )
        ));

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
        String rssXml = "<rss></rss>";

        when(hankyungRssClient.fetchRssXml()).thenReturn(rssXml);
        when(hankyungRssParser.parse(rssXml)).thenReturn(List.of(
                new RssItem(
                        "인코딩 테스트 기사",
                        "https://www.hankyung.com/article/2026052974211?keyword=%EC%82%BC%EC%84%B1%20AI&utm_source=naver",
                        "인코딩 테스트 기사 요약",
                        "Fri, 29 May 2026 10:00:00 +0900"
                )
        ));

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
    @DisplayName("잘못된 pubDate를 가진 RSS item은 수집 결과에서 제외")
    void collect_skipInvalidPubDateItem() {
        // given
        String rssXml = "<rss></rss>";

        when(hankyungRssClient.fetchRssXml()).thenReturn(rssXml);
        when(hankyungRssParser.parse(rssXml)).thenReturn(List.of(
                new RssItem(
                        "날짜 오류 기사",
                        "https://www.hankyung.com/article/2026052974211",
                        "날짜 오류 기사 요약",
                        "invalid-date"
                )
        ));

        // when
        List<CollectedArticle> result = rssNewsProvider.collect();

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("description이 없는 RSS item은 title을 summary로 사용")
    void collect_useTitleAsSummaryWhenDescriptionIsMissing() {
        // given
        String rssXml = "<rss></rss>";

        when(hankyungRssClient.fetchRssXml()).thenReturn(rssXml);
        when(hankyungRssParser.parse(rssXml)).thenReturn(List.of(
                new RssItem(
                        "요약 없는 한국경제 기사",
                        "https://www.hankyung.com/article/202606095959i",
                        null,
                        "Tue, 09 Jun 2026 16:01:34 +0900"
                )
        ));

        // when
        List<CollectedArticle> result = rssNewsProvider.collect();

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).title()).isEqualTo("요약 없는 한국경제 기사");
        assertThat(result.get(0).summary()).isEqualTo("요약 없는 한국경제 기사");
    }
}