package com.codeit.monew.domain.article.scheduler;

import com.codeit.monew.domain.article.service.ArticleCollectService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NewsCollectScheduler {

    private final ArticleCollectService articleCollectService;

    // 서버 시작 후 10초 뒤 실행, 이후 1시간마다 실행
    @Scheduled(initialDelay = 10_000, fixedDelay = 3_600_000)
    public void collectNaverNews() {
        int savedCount = articleCollectService.collectFromNaver();
        log.info("네이버 뉴스 스케줄 수집 완료. savedCount={}", savedCount);
    }
}
