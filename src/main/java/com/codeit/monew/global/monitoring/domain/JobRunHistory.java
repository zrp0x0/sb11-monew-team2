package com.codeit.monew.global.monitoring.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "job_run_history")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class JobRunHistory {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "job_name", nullable = false, length = 100)
  private String jobName;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 20)
  private JobRunStatus status;

  @Column(name = "started_at", nullable = false)
  private LocalDateTime startedAt;

  @Column(name = "ended_at")
  private LocalDateTime endedAt;

  @Column(name = "duration_ms")
  private Long durationMs;

  @Column(name = "target_date")
  private LocalDate targetDate;

  @Column(name = "total_count", nullable = false)
  private long totalCount;

  @Column(name = "success_count", nullable = false)
  private long successCount;

  @Column(name = "failed_count", nullable = false)
  private long failedCount;

  @Column(name = "skipped_count", nullable = false)
  private long skippedCount;

  @Column(name = "message", length = 1000)
  private String message;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;

  private JobRunHistory(
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
    this.jobName = jobName;
    this.status = status;
    this.startedAt = startedAt;
    this.endedAt = endedAt;
    this.durationMs = durationMs;
    this.targetDate = targetDate;
    this.totalCount = totalCount;
    this.successCount = successCount;
    this.failedCount = failedCount;
    this.skippedCount = skippedCount;
    this.message = truncate(message, 1000);
  }

  public static JobRunHistory create(
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
    return new JobRunHistory(
        jobName,
        status,
        startedAt,
        endedAt,
        durationMs,
        targetDate,
        totalCount,
        successCount,
        failedCount,
        skippedCount,
        message
    );
  }

  @PrePersist
  void prePersist() {
    if (createdAt == null) {
      createdAt = LocalDateTime.now();
    }
  }

  private static String truncate(String value, int maxLength) {
    if (value == null || value.length() <= maxLength) {
      return value;
    }
    return value.substring(0, maxLength);
  }
}
