package com.codeit.monew.global.monitoring.service;

import com.codeit.monew.global.monitoring.domain.JobRunStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record JobRunHistoryCommand(
    String jobName,
    JobRunStatus status,
    LocalDateTime startedAt,
    LocalDateTime endedAt,
    Long durationMs,
    LocalDate targetDate,
    long totalCount,
    long successCount,
    long failedCount,
    long skippedCount,
    String message
) {
}
