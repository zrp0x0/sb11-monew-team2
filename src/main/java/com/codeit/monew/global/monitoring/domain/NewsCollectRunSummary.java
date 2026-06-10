package com.codeit.monew.global.monitoring.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "news_collect_run_summary")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NewsCollectRunSummary {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "job_run_history_id", nullable = false)
  private UUID jobRunHistoryId;

  @Column(name = "interest_count", nullable = false)
  private long interestCount;

  @Column(name = "api_call_count", nullable = false)
  private long apiCallCount;

  @Column(name = "candidate_count", nullable = false)
  private long candidateCount;

  @Column(name = "unique_candidate_count", nullable = false)
  private long uniqueCandidateCount;

  @Column(name = "duplicate_count", nullable = false)
  private long duplicateCount;

  @Column(name = "saved_count", nullable = false)
  private long savedCount;

  @Column(name = "failed_count", nullable = false)
  private long failedCount;

  @Column(name = "duration_ms", nullable = false)
  private long durationMs;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;

  private NewsCollectRunSummary(
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
    this.jobRunHistoryId = jobRunHistoryId;
    this.interestCount = interestCount;
    this.apiCallCount = apiCallCount;
    this.candidateCount = candidateCount;
    this.uniqueCandidateCount = uniqueCandidateCount;
    this.duplicateCount = duplicateCount;
    this.savedCount = savedCount;
    this.failedCount = failedCount;
    this.durationMs = durationMs;
  }

  public static NewsCollectRunSummary create(
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
    return new NewsCollectRunSummary(
        jobRunHistoryId,
        interestCount,
        apiCallCount,
        candidateCount,
        uniqueCandidateCount,
        duplicateCount,
        savedCount,
        failedCount,
        durationMs
    );
  }

  @PrePersist
  void prePersist() {
    if (createdAt == null) {
      createdAt = LocalDateTime.now();
    }
  }
}
