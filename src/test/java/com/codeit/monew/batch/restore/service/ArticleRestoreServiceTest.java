package com.codeit.monew.batch.restore.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.codeit.monew.batch.restore.dto.ArticleRestoreResultResponse;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.item.ExecutionContext;

@ExtendWith(MockitoExtension.class)
public class ArticleRestoreServiceTest {

  @InjectMocks
  private ArticleRestoreService articleRestoreService;

  @Mock
  private JobLauncher jobLauncher;

  @Mock
  private Job articleRestoreJob;

  @Test
  @DisplayName("2일치 복구를 요청하면 JobLauncher가 정확히 2번 호출된다.")
  void restoreRange_LoopTest() throws Exception {
    //given
    LocalDate from = LocalDate.of(2026, 1, 1);
    LocalDate to = LocalDate.of(2026, 1, 2);

    // 가짜 JobExecution과 ExecutionContext 세팅
    JobExecution mockExecution = mock(JobExecution.class);
    ExecutionContext mockContext = mock(ExecutionContext.class);

    given(jobLauncher.run(eq(articleRestoreJob), any(JobParameters.class)))
        .willReturn(mockExecution);
    given(mockExecution.getExecutionContext())
        .willReturn(mockContext);
    given(mockContext.get("RESTORED_ARTICLE_IDS"))
        .willReturn(List.of("id-1", "id-2"));

    //when
    List<ArticleRestoreResultResponse> results = articleRestoreService.restoreRange(from, to);

    //then
    assertThat(results).hasSize(2);
    assertThat(results.get(0).restoredArticleCount()).isEqualTo(2);

    verify(jobLauncher, times(2)).run(eq(articleRestoreJob), any(JobParameters.class));
  }

  @Test
  @DisplayName("배치 실행 중 에러가 나면 최대 3번까지 재시도한다.")
  void restoreRange_RetryLogicTest() throws Exception {
    //given
    LocalDate date = LocalDate.of(2026, 1, 1);

    JobExecution mockExecution = mock(JobExecution.class);
    ExecutionContext mockContext = mock(ExecutionContext.class);

    given(jobLauncher.run(any(), any()))
        .willThrow(new JobParametersInvalidException("1차 실패"))
        .willThrow(new JobParametersInvalidException("2차 실패"))
        .willReturn(mockExecution);
    given(mockExecution.getExecutionContext())
        .willReturn(mockContext);
    given(mockContext.get("RESTORED_ARTICLE_IDS"))
        .willReturn(List.of("id-1"));

    //when
    List<ArticleRestoreResultResponse> results = articleRestoreService.restoreRange(date, date);

    //then
    assertThat(results).hasSize(1);

    verify(jobLauncher, times(3)).run(any(), any());
  }

  @Test
  @DisplayName("특정 날짜가 3번 다 실패해도 시스템이 죽지 않고 무사히 다음 날짜 복구를 이어서 진행한다.")
  void restoreRange_FailAllRetries_ContinueNextDateTest() throws Exception {
    //given
    LocalDate from = LocalDate.of(2026, 1, 1);
    LocalDate to = LocalDate.of(2026, 1, 2);

    JobExecution mockExecution = mock(JobExecution.class);
    ExecutionContext mockContext = mock(ExecutionContext.class);

    given(jobLauncher.run(any(), any()))
        .willThrow(new JobParametersInvalidException("6/1 - 1차 실패"))
        .willThrow(new JobParametersInvalidException("6/1 - 2차 실패"))
        .willThrow(new JobParametersInvalidException("6/1 - 3차 실패"))
        .willReturn(mockExecution); // 6/2 - 1차 성공
    given(mockExecution.getExecutionContext())
        .willReturn(mockContext);
    given(mockContext.get("RESTORED_ARTICLE_IDS"))
        .willReturn(List.of("id-1"));

    //when
    List<ArticleRestoreResultResponse> results = articleRestoreService.restoreRange(from, to);

    //then
    assertThat(results).hasSize(1);
    assertThat(results.get(0).restoredArticleCount()).isEqualTo(1);

    verify(jobLauncher, times(4)).run(any(), any());
  }
}
