package com.codeit.monew.global.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.config.ScheduledTaskHolder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SchedulingStatusLogger {

  private final Environment environment;
  private final ObjectProvider<ScheduledTaskHolder> scheduledTaskHolders;

  @EventListener(ApplicationReadyEvent.class)
  public void logSchedulingStatus() {
    String enabled = environment.getProperty("scheduling.enabled", "true");
    String zone = environment.getProperty("scheduling.zone", "Asia/Seoul");
    String poolSize = environment.getProperty("scheduling.pool-size", "4");
    int registeredTasks = scheduledTaskHolders.stream()
        .mapToInt(holder -> holder.getScheduledTasks().size())
        .sum();

    log.info(
        "[scheduler] enabled={}, zone={}, poolSize={}, registeredTasks={}",
        enabled,
        zone,
        poolSize,
        registeredTasks
    );
  }
}
