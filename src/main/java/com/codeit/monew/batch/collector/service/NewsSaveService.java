package com.codeit.monew.batch.collector.service;

import com.codeit.monew.batch.collector.provider.CollectedNewsDto;
import com.codeit.monew.domain.article.entity.Article;
import com.codeit.monew.domain.article.entity.ArticleInterest;
import com.codeit.monew.domain.article.repository.ArticleInterestRepository;
import com.codeit.monew.domain.article.repository.ArticleRepository;
import com.codeit.monew.domain.interest.entity.Interest;
import com.codeit.monew.domain.interest.repository.InterestRepository;
import com.codeit.monew.domain.notification.listener.NotificationCreateEvent;
import com.codeit.monew.domain.subscription.repository.SubscriptionRepository;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
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
    private final ArticleInterestRepository articleInterestRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public int saveUniqueArticles(List<CollectedNewsDto> candidates) {
        if (candidates.isEmpty()) {
            return 0;
        }

        // 필요한 관심사(Interest) 엔티티들 한 번에 조회
        Set<UUID> allInterestIds = candidates.stream()
            .flatMap(dto -> dto.interestIds().stream())
            .collect(Collectors.toSet());
        Map<UUID, Interest> interestMap = interestRepository.findAllById(allInterestIds).stream()
            .collect(Collectors.toMap(Interest::getId, i -> i));

        // 수집된 URL 목록 추출 및 500개씩 분할 조회
        List<String> targetUrls = candidates.stream()
            .map(CollectedNewsDto::sourceUrl)
            .toList();

        Map<String, Article> existingArticleMap = new HashMap<>();
        int batchSize = 500;
        for (int i = 0; i < targetUrls.size(); i += batchSize) {
            List<String> subList = targetUrls.subList(i,
                Math.min(i + batchSize, targetUrls.size()));
            List<Article> existingArticles = articleRepository.findBySourceUrlIn(subList);
            for (Article article : existingArticles) {
                existingArticleMap.put(article.getSourceUrl(), article);
            }
        }

        // 기존 기사들의 이미 존재하는 매핑 정보 조회 (Unique 제약조건 위배 방지)
        List<UUID> existingArticleIds = existingArticleMap.values().stream()
            .map(Article::getId)
            .toList();

        Map<UUID, Set<UUID>> existingMappingMap = new HashMap<>();
        if (!existingArticleIds.isEmpty()) {
            List<ArticleInterest> existingMappings = articleInterestRepository.findByArticleIdIn(
                existingArticleIds);
            for (ArticleInterest ai : existingMappings) {
                existingMappingMap.computeIfAbsent(ai.getArticle().getId(), k -> new HashSet<>())
                    .add(ai.getInterest().getId());
            }
        }

        List<Article> newArticlesToSave = new ArrayList<>();
        List<ArticleInterest> newMappingsToSave = new ArrayList<>();
        Map<UUID, Integer> newNotificationCountMap = new HashMap<>();

        // 신규 기사만 먼저 생성하고 DB에 저장하여 ID를 확정
        for (CollectedNewsDto dto : candidates) {
            Article article = existingArticleMap.get(dto.sourceUrl());
            if (article == null) {
                article = Article.create(dto.source(), dto.sourceUrl(), dto.title(), dto.summary(),
                    dto.publishDate());
                newArticlesToSave.add(article);
                existingArticleMap.put(dto.sourceUrl(), article);
            }
        }

        // 신규 기사 선 영속화 (ID 생성)
        if (!newArticlesToSave.isEmpty()) {
            articleRepository.saveAll(newArticlesToSave);
            articleRepository.flush(); // 즉시 DB에 쿼리
        }

        // ID가 모두 확정된 상태에서 매핑 작업 수행
        for (CollectedNewsDto dto : candidates) {
            Article article = existingArticleMap.get(dto.sourceUrl());
            // 새로 저장되었거나 기존에 있던 기사의 ID 유무 확인
            boolean isNewArticle = newArticlesToSave.contains(article);

            for (UUID interestId : dto.interestIds()) {
                Interest interest = interestMap.get(interestId);
                if (interest == null) {
                    continue;
                }

                if (!isNewArticle) {
                    Set<UUID> mappedInterests = existingMappingMap.getOrDefault(article.getId(),
                        Collections.emptySet());
                    if (mappedInterests.contains(interestId)) {
                        continue;
                    }
                }

                newMappingsToSave.add(ArticleInterest.create(article, interest));
                newNotificationCountMap.merge(interestId, 1, Integer::sum);

                if (!isNewArticle) {
                    existingMappingMap.computeIfAbsent(article.getId(), k -> new HashSet<>())
                        .add(interestId);
                }
            }
        }

        // 매핑 정보 일괄 저장
        if (!newMappingsToSave.isEmpty()) {
            articleInterestRepository.saveAll(newMappingsToSave);
        }

        // 알림 처리
        if (!newNotificationCountMap.isEmpty()) {
            sendNotificationToSubscribers(newNotificationCountMap);
        }

        return newArticlesToSave.size();
    }

    private void sendNotificationToSubscribers(Map<UUID, Integer> notificationCountMap) {
        for (Map.Entry<UUID, Integer> entry : notificationCountMap.entrySet()) {
            UUID interestId = entry.getKey();
            int newArticleCount = entry.getValue();

            Interest interest = interestRepository.findById(interestId).orElse(null);
            if (interest == null) {
                continue;
            }

            List<UUID> subscriberIds = subscriptionRepository.findUserIdsByInterestId(interestId);
            if (subscriberIds.isEmpty()) {
                continue;
            }

            String notificationContent = String.format("[%s]와 관련된 기사가 %d건 등록되었습니다.",
                interest.getName(), newArticleCount);

            for (UUID receiverId : subscriberIds) {
                eventPublisher.publishEvent(
                    NotificationCreateEvent.createEventByArticleCollect(interest, newArticleCount,
                        receiverId)
                );
            }

            log.info("[관심사 수집 알림 이벤트 발행 완료] 관심사명: {}, 신규 기사: {}건, 발송 대상 구독자: {}명",
                interest.getName(), newArticleCount, subscriberIds.size());
        }
    }
}