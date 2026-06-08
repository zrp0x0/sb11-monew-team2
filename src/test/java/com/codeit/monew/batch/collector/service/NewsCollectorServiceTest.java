package com.codeit.monew.batch.collector.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
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
import java.util.Set;
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
        newsCollectorService = new NewsCollectorService(
            interestRepository,
            newsSaveService,
            List.of(mockProvider1, mockProvider2)
        );
    }

    @Test
    @DisplayName("관심사가 없으면 공급자 호출 없이 뉴스 수집 배치를 종료한다")
    void collectNewsHourly_NoInterests_Terminated() {
        // given
        when(interestRepository.findAllWithKeywords()).thenReturn(Collections.emptyList());

        // when
        newsCollectorService.collectNewsHourly();

        // then
        verify(mockProvider1, never()).fetchNews(any());
        verify(newsSaveService, never()).saveUniqueArticles(any());
    }

    @Test
    @DisplayName("동일한 URL의 기사는 하나로 합치고 관심사 ID를 병합해 저장한다")
    void collectNewsHourly_DuplicateUrls_DeDuplicatedBeforeSave() {
        // given
        UUID interestId1 = UUID.randomUUID();
        UUID interestId2 = UUID.randomUUID();

        Interest interest = mock(Interest.class);
        when(interestRepository.findAllWithKeywords()).thenReturn(List.of(interest));

        // 두 공급자가 동일한 URL이지만 서로 다른 관심사 ID를 가진 기사를 반환한다고 가정
        CollectedNewsDto newsFromProvider1 = new CollectedNewsDto(
            ArticleSource.NAVER, "https://news.url/1", "중복 기사", LocalDateTime.now(), "본문",
            Set.of(interestId1)
        );
        CollectedNewsDto newsFromProvider2 = new CollectedNewsDto(
            ArticleSource.NAVER, "https://news.url/1", "중복 기사", LocalDateTime.now(), "본문",
            Set.of(interestId2)
        );

        when(mockProvider1.fetchNews(interest)).thenReturn(List.of(newsFromProvider1));
        when(mockProvider2.fetchNews(interest)).thenReturn(List.of(newsFromProvider2));

        // when
        newsCollectorService.collectNewsHourly();

        // then
        // 중복이 제거되어 사이즈가 1인 리스트가 넘어가며, 두 관심사 ID가 모두 병합되었는지 검증
        verify(newsSaveService, times(1)).saveUniqueArticles(argThat(list -> {
            if (list.size() != 1) {
                return false;
            }
            CollectedNewsDto mergedDto = list.get(0);
            return mergedDto.interestIds().contains(interestId1) &&
                mergedDto.interestIds().contains(interestId2) &&
                mergedDto.interestIds().size() == 2;
        }));
    }
}
