package com.codeit.monew.domain.article.collector;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RssArticleCollectScheduler {

    private final RssProperties rssProperties;
    private final RssArticleCollector rssArticleCollector;

    @Scheduled(cron = "${monew.rss.cron:0 0 * * * *}")
    public void collect() {
        if (!rssProperties.isEnabled()) {
            log.debug("RSS 기사 자동 수집이 비활성화되어 있습니다.");
            return;
        }

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