package com.codeit.monew.global.monitoring.dto;

import java.time.LocalDateTime;

public record BatchStepExecutionSummary(
    long stepExecutionId,
    String stepName,
    String status,
    LocalDateTime startedAt,
    LocalDateTime endedAt,
    Long durationMs,
    long readCount,
    long writeCount,
    long skipCount,
    String exitCode,
    String exitMessage
) {
}
