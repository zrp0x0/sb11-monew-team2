package com.codeit.monew.batch.collector.service;

import com.codeit.monew.batch.collector.provider.CollectedNewsDto;
import com.codeit.monew.domain.article.entity.Article;
import com.codeit.monew.domain.article.repository.ArticleRepository;
import com.codeit.monew.domain.interest.entity.Interest;
import com.codeit.monew.domain.interest.repository.InterestRepository;
import com.codeit.monew.domain.notification.entity.NotificationResourceType;
import com.codeit.monew.domain.notification.listener.NotificationCreateEvent;
import com.codeit.monew.domain.subscription.repository.SubscriptionRepository;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class NewsSaveService {

    private final InterestRepository interestRepository;
    private final ArticleRepository articleRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final ApplicationEventPublisher eventPublisher;


    /**
     * 수집된 대량의 데이터를 가져와 닥 한 번만 DB를 조회해 중복을 제거한 뒤 일괄 저장함 외부 API 통신이 끝난 이후이므로 트랜잭션이 물려있지 않게 됨
     */
    @Transactional
    public int saveUniqueArticles(List<CollectedNewsDto> candidates) {
        if (candidates.isEmpty()) {
            return 0;
        }

        List<String> targetUrls = candidates.stream()
            .map(CollectedNewsDto::sourceUrl)
            .toList();

        // 500개 단위 쪼개어 조회 (PostgreSQL 인덱스 최적화 및 파라미터 한도 해제)
        Set<String> alreadySavedUrls = new HashSet<>();
        int batchSize = 500;
        for (int i = 0; i < targetUrls.size(); i += batchSize) {
            List<String> subList = targetUrls.subList(i,
                Math.min(i + batchSize, targetUrls.size()));
            List<Article> existingArticles = articleRepository.findBySourceUrlIn(subList);
            for (Article article : existingArticles) {
                alreadySavedUrls.add(article.getSourceUrl());
            }
        }

        List<Article> articlesToSave = new ArrayList<>();
        List<CollectedNewsDto> newlySavedNewsList = new ArrayList<>();
        for (CollectedNewsDto dto : candidates) {
            if (alreadySavedUrls.contains(dto.sourceUrl())) {
                continue;
            }

            Article article = Article.create(
                dto.source(),
                dto.sourceUrl(),
                dto.title(),
                dto.summary(),
                dto.publishDate()
            );
            articlesToSave.add(article);
            newlySavedNewsList.add(dto);
        }

        if (!articlesToSave.isEmpty()) {
            articleRepository.saveAll(articlesToSave);

            // TODO: 알림
            Map<UUID, List<CollectedNewsDto>> groupedByInterest = newlySavedNewsList.stream()
                .collect(Collectors.groupingBy(CollectedNewsDto::interestId));

            sendNotificationToSubscribers(groupedByInterest);

            return articlesToSave.size();
        }

        return 0;
    }

    private void sendNotificationToSubscribers(
        Map<UUID, List<CollectedNewsDto>> groupedByInterest) {

        for (Map.Entry<UUID, List<CollectedNewsDto>> entry : groupedByInterest.entrySet()) {
            UUID interestId = entry.getKey();
            int newArticleCount = entry.getValue().size();

            Interest interest = interestRepository.findById(interestId).orElse(null);
            if (interest == null) {
                continue;
            }

            List<UUID> subscriberIds = subscriptionRepository.findUserIdsByInterestId(
                interestId);
            if (subscriberIds.isEmpty()) {
                continue;
            }

            String notificationContent = String.format("[%s]와 관련된 기사가 %d건 등록되었습니다.",
                interest.getName(), newArticleCount);

            for (UUID receiverId : subscriberIds) {
                eventPublisher.publishEvent(new NotificationCreateEvent(
                    receiverId,
                    notificationContent,
                    NotificationResourceType.ARTICLE, // 알림 종류 메타데이터
                    interestId            // 알림 클릭 시 이동할 관심사 피드 ID
                ));
            }

            log.info("[관심사 수집 알림 이벤트 발행 완료] 관심사명: {}, 신규 기사: {}건, 발송 대상 구독자: {}명",
                interest.getName(), newArticleCount, subscriberIds.size());
        }
    }
}