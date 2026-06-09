package com.codeit.monew.domain.article.collector;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.codeit.monew.domain.article.collector.HankyungRssParser.RssItem;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class HankyungRssParserTest {

    private final HankyungRssParser hankyungRssParser = new HankyungRssParser();

    @Test
    @DisplayName("RSS XML에서 item 목록 파싱")
    void parse_returnRssItems() {
        // given
        String rssXml = """
                <rss>
                    <channel>
                        <item>
                            <title>첫 번째 기사</title>
                            <link>https://www.hankyung.com/article/1</link>
                            <description>첫 번째 기사 요약</description>
                            <pubDate>Fri, 29 May 2026 10:00:00 +0900</pubDate>
                        </item>
                        <item>
                            <title>두 번째 기사</title>
                            <link>https://www.hankyung.com/article/2</link>
                            <description>두 번째 기사 요약</description>
                            <pubDate>Fri, 29 May 2026 11:00:00 +0900</pubDate>
                        </item>
                    </channel>
                </rss>
                """;

        // when
        List<RssItem> result = hankyungRssParser.parse(rssXml);

        // then
        assertThat(result).hasSize(2);

        assertThat(result.get(0).title()).isEqualTo("첫 번째 기사");
        assertThat(result.get(0).link()).isEqualTo("https://www.hankyung.com/article/1");
        assertThat(result.get(0).description()).isEqualTo("첫 번째 기사 요약");
        assertThat(result.get(0).pubDate()).isEqualTo("Fri, 29 May 2026 10:00:00 +0900");

        assertThat(result.get(1).title()).isEqualTo("두 번째 기사");
    }

    @Test
    @DisplayName("RSS item에 일부 태그가 없으면 null 파싱")
    void parse_returnNullWhenTagIsMissing() {
        // given
        String rssXml = """
                <rss>
                    <channel>
                        <item>
                            <title>링크 없는 기사</title>
                            <description>링크 없는 기사 요약</description>
                        </item>
                    </channel>
                </rss>
                """;

        // when
        List<RssItem> result = hankyungRssParser.parse(rssXml);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).title()).isEqualTo("링크 없는 기사");
        assertThat(result.get(0).link()).isNull();
        assertThat(result.get(0).description()).isEqualTo("링크 없는 기사 요약");
        assertThat(result.get(0).pubDate()).isNull();
    }

    @Test
    @DisplayName("잘못된 RSS XML이면 예외 발생")
    void parse_throwExceptionWhenXmlIsInvalid() {
        // given
        String invalidXml = "<rss><channel><item></rss>";

        // when & then
        assertThatThrownBy(() -> hankyungRssParser.parse(invalidXml))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("한국경제 RSS 파싱에 실패했습니다.");
    }
}