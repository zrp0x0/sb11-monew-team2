package com.codeit.monew.global.monitoring.dto;

import java.time.LocalDateTime;
import java.util.List;

public record BatchJobExecutionSummary(
    long jobExecutionId,
    String jobName,
    String status,
    LocalDateTime startedAt,
    LocalDateTime endedAt,
    Long durationMs,
    String targetDate,
    String exitCode,
    String exitMessage,
    List<BatchStepExecutionSummary> steps
) {
}
