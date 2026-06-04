package com.codeit.monew.batch.collector.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.codeit.monew.batch.collector.provider.CollectedNewsDto;
import com.codeit.monew.batch.collector.provider.NewsProvider;
import com.codeit.monew.domain.article.entity.ArticleSource;
import com.codeit.monew.domain.interest.entity.Interest;
import com.codeit.monew.domain.interest.repository.InterestRepository;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NewsCollectorServiceTest {

    @Mock
    private InterestRepository interestRepository;
    @Mock
    private NewsSaveService newsSaveService;

    @Mock
    private NewsProvider mockProvider1;
    @Mock
    private NewsProvider mockProvider2;

    private NewsCollectorService newsCollectorService;

    @BeforeEach
    void setUp() {
        // Provider 리스트를 수동으로 주입
        newsCollectorService = new NewsCollectorService(
            interestRepository,
            newsSaveService,
            List.of(mockProvider1, mockProvider2)
        );
    }

    @Test
    @DisplayName("등록된 관심사가 없으면, 공급자를 조회하지 않고 즉시 배치를 종료한다.")
    void collectNewsHourly_NoInterests_Terminated() {
        // given
        when(interestRepository.findAll()).thenReturn(Collections.emptyList());

        // when
        newsCollectorService.collectNewsHourly();

        // then
        verify(mockProvider1, never()).fetchNews(any());
        verify(newsSaveService, never()).saveUniqueArticles(any());
    }

    @Test
    @DisplayName("서로 다른 공급자에서 동일한 URL의 기사가 수집될 경우, Map을 통해 단건화 처리되어 저장 서비스로 넘어간다.")
    void collectNewsHourly_DuplicateUrls_DeDuplicatedBeforeSave() {
        // given
        UUID interestId = UUID.randomUUID();
        Interest interest = Interest.create("경제", List.of("주식"));

        when(interestRepository.findAll()).thenReturn(List.of(interest));
        when(mockProvider1.getSource()).thenReturn(ArticleSource.NAVER);
        when(mockProvider2.getSource()).thenReturn(ArticleSource.NAVER);

        // 동일한 URL을 가진 가짜 수집 기사
        CollectedNewsDto commonNews = new CollectedNewsDto(
            ArticleSource.NAVER, "https://news.url/1", "중복 기사", LocalDateTime.now(), "본문",
            interestId
        );

        when(mockProvider1.fetchNews(interest)).thenReturn(List.of(commonNews));
        when(mockProvider2.fetchNews(interest)).thenReturn(List.of(commonNews));

        // when
        newsCollectorService.collectNewsHourly();

        // then
        // 중복이 제거되어 사이즈가 1인 리스트가 saveUniqueArticles로 넘어가는지 검증
        verify(newsSaveService, times(1)).saveUniqueArticles(argThat(list -> list.size() == 1));
    }
}