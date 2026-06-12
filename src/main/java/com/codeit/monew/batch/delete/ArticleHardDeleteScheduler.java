package com.codeit.monew.batch.delete;

import com.codeit.monew.batch.delete.service.ArticleHardDeleteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class ArticleHardDeleteScheduler {

    private final ArticleHardDeleteService articleHardDeleteService;

    // 매번 삭제할 데이터 건수 조절 (청크 단위)
    private static final int BATCH_SIZE = 1000;

    /**
     * 매일 자정(00시 00분 00초)에 실행
     */
    @Scheduled(cron = "0 0 0 * * *")
    public void executeDailyArticleCleanup() {
        LocalDateTime cutoffDate = LocalDateTime.now().minusMonths(1);
        int totalDeleted = 0;

        log.info("Starting looping hard delete for articles created before: {}", cutoffDate);

        while (true) {
            try {
                int deletedCount = articleHardDeleteService.deleteOldArticlesBatch(cutoffDate, BATCH_SIZE);
                totalDeleted += deletedCount;

                log.info("Deleted {} articles in this batch... Total deleted: {}", deletedCount, totalDeleted);

                if (deletedCount < BATCH_SIZE) {
                    break;
                }

            } catch (Exception e) {
                log.error("Error occurred during batched hard delete loop", e);
                break;
            }
        }

        log.info("기사 물리 삭제 총 {}개 성공.", totalDeleted);
    }
}