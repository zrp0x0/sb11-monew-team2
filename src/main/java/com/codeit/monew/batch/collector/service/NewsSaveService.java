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

        Set<UUID> allInterestIds = candidates.stream()
            .flatMap(dto -> dto.interestIds().stream())
            .collect(Collectors.toSet());
        Map<UUID, Interest> interestMap = interestRepository.findAllById(allInterestIds).stream()
            .collect(Collectors.toMap(Interest::getId, interest -> interest));
        log.info("[news-save] 후보 기사 {}건, 매칭 관심사 {}개 중 {}개를 조회했습니다.",
            candidates.size(), allInterestIds.size(), interestMap.size());

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
        log.info("[news-save] 이미 저장된 기사 {}건을 확인했습니다.", existingArticleMap.size());

        List<UUID> existingArticleIds = existingArticleMap.values().stream()
            .map(Article::getId)
            .toList();

        Map<UUID, Set<UUID>> existingMappingMap = new HashMap<>();
        if (!existingArticleIds.isEmpty()) {
            List<ArticleInterest> existingMappings = articleInterestRepository.findByArticleIdIn(
                existingArticleIds);
            for (ArticleInterest articleInterest : existingMappings) {
                existingMappingMap
                    .computeIfAbsent(articleInterest.getArticle().getId(), id -> new HashSet<>())
                    .add(articleInterest.getInterest().getId());
            }
        }

        List<Article> newArticlesToSave = new ArrayList<>();
        List<ArticleInterest> newMappingsToSave = new ArrayList<>();
        Map<UUID, Integer> newNotificationCountMap = new HashMap<>();

        for (CollectedNewsDto dto : candidates) {
            Article article = existingArticleMap.get(dto.sourceUrl());
            if (article == null) {
                article = Article.create(dto.source(), dto.sourceUrl(), dto.title(), dto.summary(),
                    dto.publishDate());
                newArticlesToSave.add(article);
                existingArticleMap.put(dto.sourceUrl(), article);
            }
        }

        log.info("[news-save] 신규 저장 대상 기사 {}건입니다.", newArticlesToSave.size());
        if (!newArticlesToSave.isEmpty()) {
            articleRepository.saveAll(newArticlesToSave);
            articleRepository.flush();
        }

        for (CollectedNewsDto dto : candidates) {
            Article article = existingArticleMap.get(dto.sourceUrl());
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
                    existingMappingMap.computeIfAbsent(article.getId(), id -> new HashSet<>())
                        .add(interestId);
                }
            }
        }

        if (!newMappingsToSave.isEmpty()) {
            articleInterestRepository.saveAll(newMappingsToSave);
        }

        log.info("[news-save] 신규 기사-관심사 매핑 {}건, 알림 대상 관심사 {}개입니다.",
            newMappingsToSave.size(), newNotificationCountMap.size());

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

            for (UUID receiverId : subscriberIds) {
                eventPublisher.publishEvent(
                    NotificationCreateEvent.createEventByArticleCollect(interest, newArticleCount,
                        receiverId)
                );
            }

            log.info("[news-save] 관심사 기사 수집 알림을 발행했습니다. interestName={}, newArticleCount={}, subscriberCount={}",
                interest.getName(), newArticleCount, subscriberIds.size());
        }
    }
}