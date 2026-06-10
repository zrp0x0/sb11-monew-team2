package com.codeit.monew.global.monitoring.service;

import java.util.UUID;

public record NewsCollectRunSummaryCommand(
    UUID jobRunHistoryId,
    long interestCount,
    long apiCallCount,
    long candidateCount,
    long uniqueCandidateCount,
    long duplicateCount,
    long savedCount,
    long failedCount,
    long durationMs
) {
}
