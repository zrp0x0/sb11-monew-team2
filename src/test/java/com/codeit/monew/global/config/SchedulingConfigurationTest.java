package com.codeit.monew.global.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.codeit.monew.P1MonewApplication;
import com.codeit.monew.batch.backup.scheduler.ArticleBackupScheduler;
import com.codeit.monew.batch.collector.service.NewsCollectorService;
import com.codeit.monew.global.batch.CommentHardDeleteBatchJob;
import java.lang.reflect.Method;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

class SchedulingConfigurationTest {

  private static final String SCHEDULING_ZONE = "${scheduling.zone:Asia/Seoul}";

  @Test
  @DisplayName("Scheduling is enabled only by the conditional config")
  void schedulingEnabledOnlyByConditionalConfig() {
    assertThat(P1MonewApplication.class.isAnnotationPresent(EnableScheduling.class)).isFalse();
    assertThat(SchedulingConfig.class.isAnnotationPresent(EnableScheduling.class)).isTrue();

    ConditionalOnProperty condition = SchedulingConfig.class.getAnnotation(ConditionalOnProperty.class);
    assertThat(condition.name()).containsExactly("scheduling.enabled");
    assertThat(condition.havingValue()).isEqualTo("true");
    assertThat(condition.matchIfMissing()).isTrue();
  }

  @Test
  @DisplayName("Scheduled jobs use the configured time zone")
  void scheduledJobsUseConfiguredZone() throws NoSuchMethodException {
    assertScheduled(NewsCollectorService.class, "collectNewsHourly", "0 0 * * * *");
    assertScheduled(ArticleBackupScheduler.class, "runArticleBackupJob", "0 0 3 * * *");
    assertScheduled(CommentHardDeleteBatchJob.class, "execute", "0 0 2 * * *");
  }

  @Test
  @DisplayName("Scheduler clock uses the configured time zone")
  void schedulerClockUsesConfiguredZone() {
    Clock clock = new SchedulerClockConfig().schedulerClock("Asia/Seoul");

    assertThat(clock.getZone()).isEqualTo(ZoneId.of("Asia/Seoul"));
  }

  @Test
  @DisplayName("Scheduler uses a thread pool so one long task does not block all jobs")
  void schedulerUsesThreadPool() {
    ThreadPoolTaskScheduler scheduler = new SchedulingConfig()
        .taskScheduler(4, "monew-scheduler-");

    assertThat(scheduler.getPoolSize()).isEqualTo(4);
    assertThat(scheduler.getThreadNamePrefix()).isEqualTo("monew-scheduler-");
  }

  @Test
  @DisplayName("Article backup uses the previous KST date as targetDate")
  void articleBackupSchedulerUsesKstTargetDate() throws Exception {
    JobLauncher jobLauncher = mock(JobLauncher.class);
    Job articleBackupJob = mock(Job.class);
    Clock fixedKstClock = Clock.fixed(
        Instant.parse("2026-06-05T18:00:00Z"),
        ZoneId.of("Asia/Seoul")
    );
    ArticleBackupScheduler scheduler = new ArticleBackupScheduler(
        jobLauncher,
        articleBackupJob,
        fixedKstClock
    );

    scheduler.runArticleBackupJob();

    ArgumentCaptor<JobParameters> parametersCaptor = ArgumentCaptor.forClass(JobParameters.class);
    verify(jobLauncher).run(eq(articleBackupJob), parametersCaptor.capture());
    assertThat(parametersCaptor.getValue().getString("targetDate")).isEqualTo("2026-06-05");
  }

  private static void assertScheduled(
      Class<?> type,
      String methodName,
      String expectedCron
  ) throws NoSuchMethodException {
    Method method = type.getDeclaredMethod(methodName);
    Scheduled scheduled = method.getAnnotation(Scheduled.class);

    assertThat(scheduled).isNotNull();
    assertThat(scheduled.cron()).isEqualTo(expectedCron);
    assertThat(scheduled.zone()).isEqualTo(SCHEDULING_ZONE);
  }
}
