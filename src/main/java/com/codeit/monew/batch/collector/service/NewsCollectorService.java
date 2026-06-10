package com.codeit.monew.batch.collector.service;

import com.codeit.monew.batch.collector.provider.CollectedNewsDto;
import com.codeit.monew.batch.collector.provider.NewsFetchResult;
import com.codeit.monew.batch.collector.provider.NewsFetchStatus;
import com.codeit.monew.batch.collector.provider.NewsProvider;
import com.codeit.monew.domain.interest.entity.Interest;
import com.codeit.monew.domain.interest.repository.InterestRepository;
import com.codeit.monew.global.monitoring.domain.JobRunHistory;
import com.codeit.monew.global.monitoring.domain.JobRunStatus;
import com.codeit.monew.global.monitoring.service.JobRunHistoryCommand;
import com.codeit.monew.global.monitoring.service.JobRunHistoryService;
import com.codeit.monew.global.monitoring.service.MonewMetrics;
import com.codeit.monew.global.monitoring.service.NewsCollectRunSummaryCommand;
import com.codeit.monew.global.monitoring.service.NewsCollectRunSummaryService;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NewsCollectorService {

    private static final String JOB_NAME = "newsCollector";

    private final InterestRepository interestRepository;
    private final NewsSaveService newsSaveService;
    private final List<NewsProvider> newsProviders;
    private final JobRunHistoryService jobRunHistoryService;
    private final NewsCollectRunSummaryService newsCollectRunSummaryService;
    private final MonewMetrics monewMetrics;

    @Scheduled(cron = "0 0 * * * *", zone = "${scheduling.zone:Asia/Seoul}")
    public void collectNewsHourly() {
        MDC.put("traceId", "BATCH-" + UUID.randomUUID().toString().substring(0, 8));

        LocalDateTime startedAt = LocalDateTime.now();
        long startedNanos = System.nanoTime();
        NewsCollectStats stats = new NewsCollectStats();
        JobRunStatus status = JobRunStatus.SUCCESS;
        String message = "completed";

        log.info("[news-collector] 뉴스 수집 배치를 시작합니다.");

        try {
            List<Interest> interests = interestRepository.findAllWithKeywords();
            stats.interestCount = interests.size();
            log.info("[news-collector] 관심사 {}개를 조회했습니다.", interests.size());

            if (interests.isEmpty()) {
                status = JobRunStatus.SKIPPED;
                message = "No interests";
                log.info("[news-collector] 등록된 관심사가 없어 뉴스 수집을 건너뜁니다.");
                return;
            }

            log.info("[news-collector] 관심사 {}개를 기준으로 뉴스 공급자 {}개를 조회합니다.",
                interests.size(), newsProviders.size());

            Map<String, CollectedNewsDto> totalCandidateMap = new HashMap<>();

            for (NewsProvider provider : newsProviders) {
                int fetchedByProvider = 0;
                log.info("[news-collector] 뉴스 공급자를 실행합니다. source={}", provider.getSource());

                for (Interest interest : interests) {
                    NewsFetchResult fetchResult = provider.fetchNews(interest);
                    stats.recordFetchResult(fetchResult);
                    fetchedByProvider += fetchResult.items().size();

                    for (CollectedNewsDto news : fetchResult.items()) {
                        totalCandidateMap.merge(news.sourceUrl(), news, (existing, incoming) -> {
                            Set<UUID> mergedInterestIds = new HashSet<>(existing.interestIds());
                            mergedInterestIds.addAll(incoming.interestIds());

                            return new CollectedNewsDto(
                                existing.source(),
                                existing.sourceUrl(),
                                existing.title(),
                                existing.publishDate(),
                                existing.summary(),
                                mergedInterestIds
                            );
                        });
                    }
                }

                log.info("[news-collector] 공급자에서 후보 기사 {}건을 수집했습니다. source={}",
                    fetchedByProvider, provider.getSource());
            }

            List<CollectedNewsDto> finalCandidates = new ArrayList<>(totalCandidateMap.values());
            stats.uniqueCandidateCount = finalCandidates.size();
            stats.duplicateCount = Math.max(0, stats.candidateCount - stats.uniqueCandidateCount);
            log.info("[news-collector] 중복 제거 후 후보 기사 {}건이 남았습니다.", finalCandidates.size());

            int savedCount = newsSaveService.saveUniqueArticles(finalCandidates);
            stats.savedCount = savedCount;
            log.info("[news-collector] 신규 기사 {}건을 저장했습니다.", savedCount);

            if (stats.failedCount > 0) {
                message = "completed with providerFailures=" + stats.failedCount;
            }
        } catch (Exception e) {
            status = JobRunStatus.FAILED;
            stats.failedCount++;
            message = e.getMessage();
            log.error("[news-collector] 뉴스 수집 중 오류가 발생했습니다. errorMessage={}", e.getMessage(), e);
        } finally {
            LocalDateTime endedAt = LocalDateTime.now();
            long durationMs = Duration.ofNanos(System.nanoTime() - startedNanos).toMillis();

            recordMetrics(stats, Duration.ofMillis(durationMs));
            recordRunHistory(status, startedAt, endedAt, durationMs, stats, message);

            MDC.clear();
        }
    }

    private void recordMetrics(NewsCollectStats stats, Duration duration) {
        monewMetrics.recordNewsCollectDuration(duration);
        monewMetrics.incrementNewsCollectCandidates(stats.candidateCount);
        monewMetrics.incrementNewsCollectSaved(stats.savedCount);
        monewMetrics.incrementNewsCollectFailed(stats.failedCount);
        monewMetrics.incrementNewsCollectDuplicates(stats.duplicateCount);
    }

    private void recordRunHistory(
        JobRunStatus status,
        LocalDateTime startedAt,
        LocalDateTime endedAt,
        long durationMs,
        NewsCollectStats stats,
        String message
    ) {
        try {
            JobRunHistory history = jobRunHistoryService.record(new JobRunHistoryCommand(
                JOB_NAME,
                status,
                startedAt,
                endedAt,
                durationMs,
                null,
                stats.candidateCount,
                stats.savedCount,
                stats.failedCount,
                stats.duplicateCount + stats.skippedCount,
                message
            ));

            newsCollectRunSummaryService.record(new NewsCollectRunSummaryCommand(
                history.getId(),
                stats.interestCount,
                stats.apiCallCount,
                stats.candidateCount,
                stats.uniqueCandidateCount,
                stats.duplicateCount,
                stats.savedCount,
                stats.failedCount,
                durationMs
            ));
        } catch (Exception e) {
            log.warn("[news-collector] 실행 이력 저장에 실패했습니다. errorMessage={}", e.getMessage(), e);
        }
    }

    private static class NewsCollectStats {

        private long interestCount;
        private long apiCallCount;
        private long candidateCount;
        private long uniqueCandidateCount;
        private long duplicateCount;
        private long savedCount;
        private long failedCount;
        private long skippedCount;

        private void recordFetchResult(NewsFetchResult fetchResult) {
            if (fetchResult.apiCalled()) {
                apiCallCount++;
            }

            candidateCount += fetchResult.items().size();

            if (fetchResult.status() == NewsFetchStatus.FAILED) {
                failedCount++;
            } else if (fetchResult.status() == NewsFetchStatus.SKIPPED) {
                skippedCount++;
            }
        }
    }
}
