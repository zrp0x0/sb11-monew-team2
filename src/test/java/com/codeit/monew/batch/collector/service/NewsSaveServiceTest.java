package com.codeit.monew.batch.collector.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.codeit.monew.batch.collector.provider.CollectedNewsDto;
import com.codeit.monew.domain.article.entity.Article;
import com.codeit.monew.domain.article.entity.ArticleInterest;
import com.codeit.monew.domain.article.entity.ArticleSource;
import com.codeit.monew.domain.article.repository.ArticleInterestRepository;
import com.codeit.monew.domain.article.repository.ArticleRepository;
import com.codeit.monew.domain.interest.entity.Interest;
import com.codeit.monew.domain.interest.repository.InterestRepository;
import com.codeit.monew.domain.notification.listener.NotificationCreateEvent;
import com.codeit.monew.domain.subscription.repository.SubscriptionRepository;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class NewsSaveServiceTest {

    @Mock
    private InterestRepository interestRepository;
    @Mock
    private ArticleRepository articleRepository;
    @Mock
    private ArticleInterestRepository articleInterestRepository; // 새로 추가된 모킹
    @Mock
    private SubscriptionRepository subscriptionRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private NewsSaveService newsSaveService;

    @Captor
    private ArgumentCaptor<List<Article>> articlesCaptor;

    @Captor
    private ArgumentCaptor<List<ArticleInterest>> articleInterestsCaptor; // 매핑 검증용 캡처

    @Test
    @DisplayName("DB에 이미 존재하는 기사는 제외하고 신규 기사만 저장하며, 기존/신규 기사 모두 관심사 매핑(다대다)을 추가하고 알림을 발행한다.")
    void saveUniqueArticles_Success() {
        // given
        UUID interestId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID existingArticleId = UUID.randomUUID();

        // Mock Interest (Repository 조회용)
        Interest interest = mock(Interest.class);
        when(interest.getId()).thenReturn(interestId);
        when(interest.getName()).thenReturn("테크");

        // 수집된 DTO: 하나는 기존 기사, 하나는 신규 기사
        CollectedNewsDto existingDto = new CollectedNewsDto(ArticleSource.NAVER, "url-old", "옛날뉴스",
            LocalDateTime.now(), "요약", Set.of(interestId));
        CollectedNewsDto newDto = new CollectedNewsDto(ArticleSource.NAVER, "url-new", "신규뉴스",
            LocalDateTime.now(), "요약", Set.of(interestId));
        List<CollectedNewsDto> candidates = List.of(existingDto, newDto);

        // Mock 기존 Article
        Article existingArticle = mock(Article.class);
        when(existingArticle.getSourceUrl()).thenReturn("url-old");
        when(existingArticle.getId()).thenReturn(existingArticleId);

        // Repository Mocking
        when(interestRepository.findAllById(any())).thenReturn(List.of(interest));
        when(articleRepository.findBySourceUrlIn(any())).thenReturn(List.of(existingArticle));
        // 기존 기사가 이전에 이 관심사와 매핑된 적은 없다고 가정
        when(articleInterestRepository.findByArticleIdIn(any())).thenReturn(
            Collections.emptyList());

        // Notification Mocking
        when(interestRepository.findById(interestId)).thenReturn(Optional.of(interest));
        when(subscriptionRepository.findUserIdsByInterestId(interestId)).thenReturn(
            List.of(userId));

        // when
        int savedCount = newsSaveService.saveUniqueArticles(candidates);

        // then
        // 1. Return Count (신규 기사 1건)
        assertThat(savedCount).isEqualTo(1);

        // 2. 신규 기사 저장 및 Flush 확인
        verify(articleRepository, times(1)).saveAll(articlesCaptor.capture());
        verify(articleRepository, times(1)).flush();
        assertThat(articlesCaptor.getValue())
            .hasSize(1)
            .first()
            .satisfies(article -> assertThat(article.getSourceUrl()).isEqualTo("url-new"));

        // 3. 다대다 매핑 엔티티(ArticleInterest) 2건 저장 확인 (기존 기사용 1개 + 신규 기사용 1개)
        verify(articleInterestRepository, times(1)).saveAll(articleInterestsCaptor.capture());
        assertThat(articleInterestsCaptor.getValue()).hasSize(2);

        // 4. 알림 이벤트 발행 확인
        verify(eventPublisher, times(1)).publishEvent(any(NotificationCreateEvent.class));
    }
}