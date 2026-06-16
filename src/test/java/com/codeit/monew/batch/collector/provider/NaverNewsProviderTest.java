package com.codeit.monew.batch.collector.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.codeit.monew.domain.interest.entity.Interest;
import com.codeit.monew.global.monitoring.service.MonewMetrics;
import com.codeit.monew.infra.externalapi.naver.client.NaverNewsClient;
import com.codeit.monew.infra.externalapi.naver.dto.NaverNewsResponse;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class NaverNewsProviderTest {

  @Mock
  private NaverNewsClient naverNewsClient;
  @Mock
  private MonewMetrics monewMetrics;

  private NaverNewsProvider naverNewsProvider;

  @BeforeEach
  void setUp() {
    naverNewsProvider = new NaverNewsProvider(naverNewsClient, monewMetrics);
    ReflectionTestUtils.setField(naverNewsProvider, "naverClientId", "client-id");
    ReflectionTestUtils.setField(naverNewsProvider, "naverClientSecret", "client-secret");
  }

  @Test
  @DisplayName("네이버 응답 item이 비어 있으면 EMPTY_RESPONSE로 구분")
  void fetchNews_emptyResponse() {
    Interest interest = interestWithKeywords(List.of("spring"));
    when(naverNewsClient.searchNews(anyString(), anyString(), anyString(), anyInt(), anyInt(), anyString()))
        .thenReturn(new NaverNewsResponse(null, 0, 1, 0, Collections.emptyList()));

    NewsFetchResult result = naverNewsProvider.fetchNews(interest);

    assertThat(result.status()).isEqualTo(NewsFetchStatus.EMPTY_RESPONSE);
    assertThat(result.items()).isEmpty();
    assertThat(result.apiCalled()).isTrue();
    verify(monewMetrics).incrementNaverCalls();
    verify(monewMetrics).incrementNaverEmptyResponses();
  }

  @Test
  @DisplayName("네이버 API 예외는 FAILED로 처리")
  void fetchNews_apiException() {
    Interest interest = interestWithKeywords(List.of("spring"));
    when(naverNewsClient.searchNews(anyString(), anyString(), anyString(), anyInt(), anyInt(), anyString()))
        .thenThrow(new RuntimeException("api down"));

    NewsFetchResult result = naverNewsProvider.fetchNews(interest);

    assertThat(result.status()).isEqualTo(NewsFetchStatus.FAILED);
    assertThat(result.apiCalled()).isTrue();
    verify(monewMetrics).incrementNaverCalls();
    verify(monewMetrics).incrementNaverErrors();
  }

  @Test
  @DisplayName("유효한 키워드가 없으면 API를 호출하지 않고 SKIPPED로 구분")
  void fetchNews_noValidKeywords() {
    Interest interest = interestWithKeywords(List.of(" "));

    NewsFetchResult result = naverNewsProvider.fetchNews(interest);

    assertThat(result.status()).isEqualTo(NewsFetchStatus.SKIPPED);
    assertThat(result.apiCalled()).isFalse();
    verify(naverNewsClient, never())
        .searchNews(anyString(), anyString(), anyString(), anyInt(), anyInt(), anyString());
    verify(monewMetrics, never()).incrementNaverCalls();
  }

  private Interest interestWithKeywords(List<String> keywords) {
    Interest interest = mock(Interest.class);
    when(interest.getKeywords()).thenReturn(keywords);
    return interest;
  }
}
