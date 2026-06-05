package com.codeit.monew.batch.restore.service;

import com.codeit.monew.batch.exception.BatchErrorCode;
import com.codeit.monew.batch.exception.BatchException;
import com.codeit.monew.batch.restore.dto.ArticleRestoreResultResponse;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionException;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ArticleRestoreService {

  private final JobLauncher jobLauncher;
  private final Job articleRestoreJob;

  private static final int MAX_RETRIES = 3;

  public List<ArticleRestoreResultResponse> restoreRange(LocalDate from, LocalDate to) {
    if (from.isAfter(to)) {
      throw new BatchException(BatchErrorCode.INVALID_DATE_RANGE, Map.of("reason", "시작일이 종료일보다 늦습니다."));
    }
    if (to.isAfter(LocalDate.now())) {
      throw new BatchException(BatchErrorCode.INVALID_DATE_RANGE, Map.of("reason", "미래 날짜가 포함되었습니다."));
    }

    List<ArticleRestoreResultResponse> restoreResultDtos = new ArrayList<>();

    LocalDate currentDate = from;
    while (!currentDate.isAfter(to)) {
      String targetDateStr = currentDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

      boolean isSuccess = false; // 현재 날짜의 성공 여부 플래그

      for(int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
        try {
          JobParameters params = new JobParametersBuilder()
              .addString("targetDate", targetDateStr)
              .addLong("time", System.currentTimeMillis())
              .toJobParameters();

          JobExecution jobExecution = jobLauncher.run(articleRestoreJob, params);

          List<String> restoredIds = (List<String>) jobExecution.getExecutionContext().get("RESTORED_ARTICLE_IDS");
          if (restoredIds == null) {
            restoredIds = new ArrayList<>();
          }
          restoreResultDtos.add(
              ArticleRestoreResultResponse.of(LocalDateTime.now(), restoredIds));

          isSuccess = true;
          log.info("[{}] 기사 복구 성공 (복구 건수: {})", targetDateStr, restoredIds.size());

          break;
        } catch (JobExecutionException e) {
          log.warn("[{}] 기사 복구 {}차 시도 실패: {}", targetDateStr, attempt, e.getMessage());

          if (attempt < MAX_RETRIES) {
            try {
              Thread.sleep(2000);
            } catch (InterruptedException ie) {
              Thread.currentThread().interrupt();
            }
          }
        }
      }

      if (!isSuccess) {
        log.error("[{}] 기사 복구 3회 시도 모두 실패. 다음 날짜로 건너뜁니다.", targetDateStr);
      }
      currentDate = currentDate.plusDays(1);
    }
    return restoreResultDtos;
  }
}
