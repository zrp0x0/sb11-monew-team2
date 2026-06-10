package com.codeit.monew.batch.collector.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.codeit.monew.batch.collector.provider.CollectedNewsDto;
import com.codeit.monew.batch.collector.provider.NewsFetchResult;
import com.codeit.monew.batch.collector.provider.NewsProvider;
import com.codeit.monew.domain.article.entity.ArticleSource;
import com.codeit.monew.domain.interest.entity.Interest;
import com.codeit.monew.domain.interest.repository.InterestRepository;
import com.codeit.monew.global.monitoring.domain.JobRunHistory;
import com.codeit.monew.global.monitoring.service.JobRunHistoryCommand;
import com.codeit.monew.global.monitoring.service.JobRunHistoryService;
import com.codeit.monew.global.monitoring.service.MonewMetrics;
import com.codeit.monew.global.monitoring.service.NewsCollectRunSummaryCommand;
import com.codeit.monew.global.monitoring.service.NewsCollectRunSummaryService;
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
    @Mock
    private JobRunHistoryService jobRunHistoryService;
    @Mock
    private NewsCollectRunSummaryService newsCollectRunSummaryService;
    @Mock
    private MonewMetrics monewMetrics;

    private NewsCollectorService newsCollectorService;

    @BeforeEach
    void setUp() {
        JobRunHistory history = mock(JobRunHistory.class);
        when(history.getId()).thenReturn(UUID.randomUUID());
        when(jobRunHistoryService.record(any(JobRunHistoryCommand.class))).thenReturn(history);

        newsCollectorService = new NewsCollectorService(
            interestRepository,
            newsSaveService,
            List.of(mockProvider1, mockProvider2),
            jobRunHistoryService,
            newsCollectRunSummaryService,
            monewMetrics
        );
    }

    @Test
    @DisplayName("관심사가 없으면 공급자 호출 없이 수집 배치를 종료한다")
    void collectNewsHourly_NoInterests_Terminated() {
        when(interestRepository.findAllWithKeywords()).thenReturn(Collections.emptyList());

        newsCollectorService.collectNewsHourly();

        verify(mockProvider1, never()).fetchNews(any());
        verify(newsSaveService, never()).saveUniqueArticles(any());
        verify(newsCollectRunSummaryService, times(1)).record(any(NewsCollectRunSummaryCommand.class));
    }

    @Test
    @DisplayName("동일 URL 기사는 하나로 합치고 관심사 ID를 병합한 뒤 저장한다")
    void collectNewsHourly_DuplicateUrls_DeDuplicatedBeforeSave() {
        UUID interestId1 = UUID.randomUUID();
        UUID interestId2 = UUID.randomUUID();

        Interest interest = mock(Interest.class);
        when(interestRepository.findAllWithKeywords()).thenReturn(List.of(interest));

        CollectedNewsDto newsFromProvider1 = new CollectedNewsDto(
            ArticleSource.NAVER, "https://news.url/1", "duplicate article", LocalDateTime.now(), "summary",
            Set.of(interestId1)
        );
        CollectedNewsDto newsFromProvider2 = new CollectedNewsDto(
            ArticleSource.NAVER, "https://news.url/1", "duplicate article", LocalDateTime.now(), "summary",
            Set.of(interestId2)
        );

        when(mockProvider1.fetchNews(interest))
            .thenReturn(NewsFetchResult.success(ArticleSource.NAVER, List.of(newsFromProvider1)));
        when(mockProvider2.fetchNews(interest))
            .thenReturn(NewsFetchResult.success(ArticleSource.NAVER, List.of(newsFromProvider2)));

        newsCollectorService.collectNewsHourly();

        verify(newsSaveService, times(1)).saveUniqueArticles(argThat(list -> {
            if (list.size() != 1) {
                return false;
            }
            CollectedNewsDto mergedDto = list.get(0);
            return mergedDto.interestIds().contains(interestId1) &&
                mergedDto.interestIds().contains(interestId2) &&
                mergedDto.interestIds().size() == 2;
        }));
        verify(monewMetrics).incrementNewsCollectDuplicates(1);
    }
}
