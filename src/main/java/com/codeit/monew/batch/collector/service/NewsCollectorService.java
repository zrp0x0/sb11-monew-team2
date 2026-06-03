package com.codeit.monew.batch.collector.service;

import com.codeit.monew.batch.collector.provider.CollectedNewsDto;
import com.codeit.monew.batch.collector.provider.NewsProvider;
import com.codeit.monew.domain.interest.entity.Interest;
import com.codeit.monew.domain.interest.repository.InterestRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    // 스프링 빈으로 등록되어있는 NewsProvider를 리스트로 자동으로 주입해줌
    private final List<NewsProvider> newsProviders;

    @Scheduled(cron = "0 0 * * * *")
    public void collectNewsHourly() {
        MDC.put("traceId", "BATCH-" + UUID.randomUUID().toString().substring(0, 8));
        log.info("[뉴스 수집 배치 시작] 다중 공급자 기반 시간당 뉴스 통합 배치를 시작합니다.");

        try {
            // TODO: 근데 관심사 등록된게 너무 많으면? => 이 서비스에서는 하지 고려하지 말자
            List<Interest> interests = interestRepository.findAll();
            if (interests.isEmpty()) {
                log.info("[뉴스 수집 배치] 등록된 관심사가 없어 배치를 종료합니다.");
                return;
            }

            // 공급자(Provider)가 늘어나더라도 전체 배치 턴 내에서 완벽한 단건화를 보장하기 위해 Map 사용
            Map<String, CollectedNewsDto> totalCandidateMap = new HashMap<>();

            // 등록된 모든 뉴스 소스(Naver, RSS 등)를 순회하며 수집
            for (NewsProvider provider : newsProviders) {
                log.info("[뉴스 수집 공급자 가동] 공급처: {}", provider.getSource());

                for (Interest interest : interests) {
                    List<CollectedNewsDto> fetchedNews = provider.fetchNews(interest);

                    for (CollectedNewsDto news : fetchedNews) {
                        totalCandidateMap.put(news.sourceUrl(), news);
                    }
                }
            }

            // 수집된 청정 후보군 데이터를 분리된 저장 서비스로 넘겨 일괄 중복 검증 및 저장 진행
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
