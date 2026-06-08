package com.codeit.monew.batch.collector.service;

import com.codeit.monew.batch.collector.provider.CollectedNewsDto;
import com.codeit.monew.batch.collector.provider.NewsProvider;
import com.codeit.monew.domain.interest.entity.Interest;
import com.codeit.monew.domain.interest.repository.InterestRepository;
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

    private final InterestRepository interestRepository;
    private final NewsSaveService newsSaveService;
    private final List<NewsProvider> newsProviders;

    @Scheduled(cron = "0 0 * * * *", zone = "${scheduling.zone:Asia/Seoul}")
    public void collectNewsHourly() {
        MDC.put("traceId", "BATCH-" + UUID.randomUUID().toString().substring(0, 8));
        log.info("[news-collector] 뉴스 수집 배치를 시작합니다.");

        try {
            List<Interest> interests = interestRepository.findAllWithKeywords();
            log.info("[news-collector] 관심사 {}개를 조회했습니다.", interests.size());
            if (interests.isEmpty()) {
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
                    List<CollectedNewsDto> fetchedNews = provider.fetchNews(interest);
                    fetchedByProvider += fetchedNews.size();

                    for (CollectedNewsDto news : fetchedNews) {
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
            log.info("[news-collector] 중복 제거 후 후보 기사 {}건이 남았습니다.", finalCandidates.size());

            int savedCount = newsSaveService.saveUniqueArticles(finalCandidates);
            log.info("[news-collector] 신규 기사 {}건을 저장했습니다.", savedCount);

            if (savedCount > 0) {
                log.info("[news-collector] 뉴스 수집 배치를 완료했습니다. savedArticles={}", savedCount);
            } else {
                log.info("[news-collector] 뉴스 수집 배치를 완료했습니다. 저장할 신규 기사가 없습니다.");
            }

        } catch (Exception e) {
            log.error("[news-collector] 뉴스 수집 중 오류가 발생했습니다. errorMessage={}", e.getMessage(), e);
        } finally {
            MDC.clear();
        }
    }
}
