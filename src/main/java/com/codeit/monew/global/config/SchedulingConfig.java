package com.codeit.monew.global.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Slf4j
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "scheduling.enabled", havingValue = "true", matchIfMissing = true)
public class SchedulingConfig {

  @Bean(name = "taskScheduler")
  public ThreadPoolTaskScheduler taskScheduler(
      @Value("${scheduling.pool-size:4}") int poolSize,
      @Value("${scheduling.thread-name-prefix:monew-scheduler-}") String threadNamePrefix
  ) {
    ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
    scheduler.setPoolSize(poolSize);
    scheduler.setThreadNamePrefix(threadNamePrefix);
    scheduler.setWaitForTasksToCompleteOnShutdown(true);
    scheduler.setAwaitTerminationSeconds(30);
    scheduler.setRemoveOnCancelPolicy(true);
    scheduler.setErrorHandler(error -> log.error("[스케줄러 오류] 예약 작업 실행 중 오류가 발생했습니다.", error));
    return scheduler;
  }
}
