package com.codeit.monew.batch.collector.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.codeit.monew.domain.article.entity.ArticleSource;
import com.codeit.monew.domain.interest.entity.Interest;
import com.codeit.monew.infra.externalapi.hankyung.client.HankyungRssClient;
import com.codeit.monew.infra.externalapi.hankyung.dto.HankyungRssItem;
import com.codeit.monew.infra.externalapi.hankyung.parser.HankyungRssParser;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class HankyungRssNewsProviderTest {

    @Mock
    private HankyungRssClient hankyungRssClient;

    @Mock
    private HankyungRssParser hankyungRssParser;

    @InjectMocks
    private HankyungRssNewsProvider provider;

    @Test
    @DisplayName("빈 RSS 파싱 결과도 TTL 안에서는 캐시해 반복 호출하지 않는다")
    void fetchNews_EmptyRssItems_Cached() {
        // given
        Interest interest = interest("경제", List.of("반도체"));
        when(hankyungRssClient.fetchRssXml()).thenReturn("<rss><channel></channel></rss>");
        when(hankyungRssParser.parse("<rss><channel></channel></rss>"))
                .thenReturn(Collections.emptyList());

        // when
        List<CollectedNewsDto> firstResult = provider.fetchNews(interest);
        List<CollectedNewsDto> secondResult = provider.fetchNews(interest);

        // then
        assertThat(firstResult).isEmpty();
        assertThat(secondResult).isEmpty();
        verify(hankyungRssClient, times(1)).fetchRssXml();
        verify(hankyungRssParser, times(1)).parse("<rss><channel></channel></rss>");
    }

    @Test
    @DisplayName("키워드와 매칭되는 RSS item을 정리해 수집 DTO로 변환한다")
    void fetchNews_MatchedItem_ReturnsCollectedNews() {
        // given
        UUID interestId = UUID.randomUUID();
        Interest interest = interest("AI", List.of(" AI ", "로봇"));
        ReflectionTestUtils.setField(interest, "id", interestId);

        String longSummary = "설명 ".repeat(700);
        HankyungRssItem matchedItem = new HankyungRssItem(
                "<b>AI</b> 투자 &amp; 전략",
                "HTTPS://WWW.HANKYUNG.COM/article/2026061000011/?utm_source=test&foo=bar",
                "<p>" + longSummary + "</p>",
                "Wed, 10 Jun 2026 09:15:00 +0900"
        );
        HankyungRssItem unmatchedItem = new HankyungRssItem(
                "스포츠 소식",
                "https://www.hankyung.com/article/2026061000022",
                "야구 경기 결과",
                "Wed, 10 Jun 2026 09:30:00 +0900"
        );

        when(hankyungRssClient.fetchRssXml()).thenReturn("rss-xml");
        when(hankyungRssParser.parse("rss-xml")).thenReturn(List.of(matchedItem, unmatchedItem));

        // when
        List<CollectedNewsDto> result = provider.fetchNews(interest);

        // then
        assertThat(result).hasSize(1);
        CollectedNewsDto news = result.get(0);
        assertThat(news.source()).isEqualTo(ArticleSource.HANKYUNG);
        assertThat(news.sourceUrl()).isEqualTo("https://www.hankyung.com/article/2026061000011?foo=bar");
        assertThat(news.title()).isEqualTo("AI 투자 & 전략");
        assertThat(news.summary()).hasSize(2000);
        assertThat(news.publishDate()).isEqualTo(LocalDateTime.of(2026, 6, 10, 9, 15));
        assertThat(news.interestIds()).containsExactly(interestId);
    }

    private Interest interest(String name, List<String> keywords) {
        Interest interest = Interest.create(name, keywords);
        ReflectionTestUtils.setField(interest, "id", UUID.randomUUID());
        return interest;
    }
}
