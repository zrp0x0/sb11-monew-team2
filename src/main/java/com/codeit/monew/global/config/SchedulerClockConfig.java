package com.codeit.monew.global.config;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SchedulerClockConfig {

  @Bean
  public Clock schedulerClock(@Value("${scheduling.zone:Asia/Seoul}") String schedulingZone) {
    return Clock.system(ZoneId.of(schedulingZone));
  }
}
