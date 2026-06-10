package com.codeit.monew.infra.externalapi.hankyung.parser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.codeit.monew.infra.externalapi.hankyung.dto.HankyungRssItem;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class HankyungRssParserTest {

    private final HankyungRssParser parser = new HankyungRssParser();

    @Test
    @DisplayName("RSS XML에서 item 필드를 추출한다")
    void parse_ValidRssXml_ReturnsItems() {
        // given
        String rssXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <rss version="2.0">
                  <channel>
                    <item>
                      <title><![CDATA[AI &amp; 반도체]]></title>
                      <link>https://www.hankyung.com/article/2026061000011</link>
                      <description><![CDATA[<p>시장 확대</p>]]></description>
                      <pubDate>Wed, 10 Jun 2026 09:15:00 +0900</pubDate>
                    </item>
                    <item>
                      <title>경제 뉴스</title>
                      <link>https://www.hankyung.com/article/2026061000022</link>
                      <description>요약</description>
                      <pubDate>Wed, 10 Jun 2026 10:15:00 +0900</pubDate>
                    </item>
                  </channel>
                </rss>
                """;

        // when
        List<HankyungRssItem> result = parser.parse(rssXml);

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).title()).isEqualTo("AI &amp; 반도체");
        assertThat(result.get(0).link()).isEqualTo("https://www.hankyung.com/article/2026061000011");
        assertThat(result.get(0).description()).isEqualTo("<p>시장 확대</p>");
        assertThat(result.get(0).pubDate()).isEqualTo("Wed, 10 Jun 2026 09:15:00 +0900");
    }

    @Test
    @DisplayName("DOCTYPE이 포함된 XML은 파싱하지 않는다")
    void parse_XmlWithDoctype_ThrowsException() {
        // given
        String rssXml = """
                <?xml version="1.0"?>
                <!DOCTYPE rss [
                  <!ENTITY xxe SYSTEM "file:///etc/passwd">
                ]>
                <rss><channel><item><title>&xxe;</title></item></channel></rss>
                """;

        // when & then
        assertThatThrownBy(() -> parser.parse(rssXml))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("한국경제 RSS 파싱에 실패했습니다.");
    }
}
