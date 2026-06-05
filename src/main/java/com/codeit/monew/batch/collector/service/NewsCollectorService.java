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

    @Scheduled(cron = "0 0 * * * *")
    public void collectNewsHourly() {
        MDC.put("traceId", "BATCH-" + UUID.randomUUID().toString().substring(0, 8));
        log.info("[뉴스 수집 배치 시작] 다중 공급자 기반 시간당 뉴스 통합 배치를 시작합니다.");

        try {
            List<Interest> interests = interestRepository.findAll();
            if (interests.isEmpty()) {
                log.info("[뉴스 수집 배치] 등록된 관심사가 없어 배치를 종료합니다.");
                return;
            }

            Map<String, CollectedNewsDto> totalCandidateMap = new HashMap<>();

            for (NewsProvider provider : newsProviders) {
                log.info("[뉴스 수집 공급자 가동] 공급처: {}", provider.getSource());

                for (Interest interest : interests) {
                    List<CollectedNewsDto> fetchedNews = provider.fetchNews(interest);

                    for (CollectedNewsDto news : fetchedNews) {
                        // 중복 기사 발견 시, 기존 데이터의 interestIds에 새로운 interestId를 병합(Merge)
                        totalCandidateMap.merge(news.sourceUrl(), news, (existing, incoming) -> {
                            // 불변 Set 예외를 방지하기 위해 새로운 가변 Set 생성 후 병합
                            Set<UUID> mergedInterestIds = new HashSet<>(existing.interestIds());
                            mergedInterestIds.addAll(incoming.interestIds());

                            return new CollectedNewsDto(
                                existing.source(),
                                existing.sourceUrl(),
                                existing.title(),
                                existing.publishDate(),
                                existing.summary(),
                                mergedInterestIds // 새로운 Set 적용
                            );
                        });
                    }
                }
            }

            List<CollectedNewsDto> finalCandidates = new ArrayList<>(totalCandidateMap.values());
            int savedCount = newsSaveService.saveUniqueArticles(finalCandidates);

            if (savedCount > 0) {
                log.info("[뉴스 수집 배치 성공] 총 {}건의 신규 기사를 저장했습니다.", savedCount);
            } else {
                log.info("[뉴스 수집 배치 완료] 새롭게 추가된 신규 기사가 없습니다.");
            }

        } catch (Exception e) {
            log.error("[뉴스 수집 배치 에러] 원인: {}", e.getMessage(), e);
        } finally {
            MDC.clear();
        }
    }
}