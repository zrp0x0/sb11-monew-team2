package com.codeit.monew.domain.article.collector;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "monew.rss",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class RssArticleCollectScheduler {

    private final RssArticleCollector rssArticleCollector;

    @Scheduled(cron = "${monew.rss.cron:0 0 * * * *}")
    public void collect() {
        RssArticleCollector.CollectResult result = rssArticleCollector.collect();

        log.info(
                "RSS 기사 자동 수집 완료. total={}, saved={}, skipped={}, failed={}",
                result.totalCount(),
                result.savedCount(),
                result.skippedCount(),
                result.failedCount()
        );
    }
}