package com.codeit.monew.batch.backup.scheduler;

import com.codeit.monew.batch.exception.BatchErrorCode;
import com.codeit.monew.batch.exception.BatchException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ArticleBackupScheduler {

  private final JobLauncher jobLauncher;
  private final Job articleBackupJob;
  private final Clock schedulerClock;

  @Scheduled(cron = "0 0 3 * * *", zone = "${scheduling.zone:Asia/Seoul}")
  public void runArticleBackupJob() {
    String targetDate = LocalDate.now(schedulerClock).minusDays(1)
        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

    JobParameters jobParameters = new JobParametersBuilder()
        .addString("targetDate", targetDate)
        .addLong("time", System.currentTimeMillis())
        .toJobParameters();

    try {
      log.info("[{}] 기사 백업 배치 시작", targetDate);
      jobLauncher.run(articleBackupJob, jobParameters);
      log.info("[{}] 기사 백업 배치 완료", targetDate);
    } catch (Exception e) {
      log.error("[{}] 기사 백업 배치 실패: {}", targetDate, e.getMessage());
      throw new BatchException(BatchErrorCode.BACKUP_JOB_FAILED, Map.of("reason", e.getMessage()));
    }
  }
}
