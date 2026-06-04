package com.codeit.monew.batch.collector.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.codeit.monew.batch.collector.provider.CollectedNewsDto;
import com.codeit.monew.domain.article.entity.Article;
import com.codeit.monew.domain.article.entity.ArticleSource;
import com.codeit.monew.domain.article.repository.ArticleRepository;
import com.codeit.monew.domain.interest.entity.Interest;
import com.codeit.monew.domain.interest.repository.InterestRepository;
import com.codeit.monew.domain.notification.listener.NotificationCreateEvent;
import com.codeit.monew.domain.subscription.repository.SubscriptionRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
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
    private SubscriptionRepository subscriptionRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private NewsSaveService newsSaveService;
    
    @Captor
    private ArgumentCaptor<Iterable<Article>> articlesCaptor;

    @Test
    @DisplayName("DB에 이미 존재하는 기사는 제외하고, 신규 기사만 저장하며 구독자에게 알림 이벤트를 발행한다.")
    void saveUniqueArticles_Success() {
        // given
        UUID interestId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Interest interest = Interest.create("테크", List.of("개발"));

        CollectedNewsDto existingDto = new CollectedNewsDto(ArticleSource.NAVER, "url-old", "옛날뉴스",
            LocalDateTime.now(), "요약", interestId);
        CollectedNewsDto newDto = new CollectedNewsDto(ArticleSource.NAVER, "url-new", "신규뉴스",
            LocalDateTime.now(), "요약", interestId);
        List<CollectedNewsDto> candidates = List.of(existingDto, newDto);

        // url-old는 이미 DB에 있다고 가정
        Article existingArticle = Article.create(ArticleSource.NAVER, "url-old", "옛날뉴스", "요약",
            LocalDateTime.now());
        when(articleRepository.findBySourceUrlIn(any())).thenReturn(List.of(existingArticle));

        when(interestRepository.findById(interestId)).thenReturn(Optional.of(interest));
        when(subscriptionRepository.findUserIdsByInterestId(interestId)).thenReturn(
            List.of(userId));

        // when
        newsSaveService.saveUniqueArticles(candidates);

        // then
        // 1. ArgumentCaptor로 saveAll에 넘어간 파라미터를 캡처
        verify(articleRepository, times(1)).saveAll(articlesCaptor.capture());

        // AssertJ를 이용해 캡처된 데이터를 상세 검증
        assertThat(articlesCaptor.getValue())
            .hasSize(1) // 한 건만 넘어갔는지 확인
            .first()
            .satisfies(article -> {
                // 저장된 기사가 옛날 뉴스가 아니라 진짜 '신규 뉴스(url-new)'가 맞는지 확인
                assertThat(article.getSourceUrl()).isEqualTo("url-new");
            });

        // 2. 신규 기사 알림 이벤트가 1회 발행되었는지 검증
        verify(eventPublisher, times(1)).publishEvent(any(NotificationCreateEvent.class));
    }
}