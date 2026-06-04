package com.codeit.monew.domain.article.collector;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
@EnabledIfEnvironmentVariable(named = "RSS_LIVE_TEST", matches = "true")
class RssArticleCollectorLiveTest {

    @Autowired
    private RssArticleCollector hankyungRssArticleCollector;

    @Test
    @DisplayName("실제 한국경제 RSS를 호출해 기사 수집 흐름을 확인한다")
    void collect_realHankyungRss() {
        // when
        RssArticleCollector.CollectResult result = hankyungRssArticleCollector.collect();

        // then
        assertThat(result.totalCount()).isGreaterThan(0);
        assertThat(result.savedCount() + result.skippedCount() + result.failedCount())
                .isEqualTo(result.totalCount());
    }
}