package com.codeit.monew.global.monitoring.service;

import com.codeit.monew.global.monitoring.dto.BatchJobExecutionSummary;
import com.codeit.monew.global.monitoring.dto.BatchStepExecutionSummary;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BatchMonitoringQueryService {

  private final JdbcTemplate jdbcTemplate;

  public Optional<BatchJobExecutionSummary> findLatestExecution(String jobName) {
    List<BatchJobExecutionSummary> executions = findRecentExecutions(jobName, 1);
    return executions.stream().findFirst();
  }

  public List<BatchJobExecutionSummary> findRecentExecutions(String jobName, int limit) {
    int safeLimit = Math.max(1, Math.min(limit, 100));
    String sql = """
        SELECT e.JOB_EXECUTION_ID,
               i.JOB_NAME,
               e.STATUS,
               e.START_TIME,
               e.END_TIME,
               e.EXIT_CODE,
               e.EXIT_MESSAGE,
               (
                   SELECT p.PARAMETER_VALUE
                   FROM BATCH_JOB_EXECUTION_PARAMS p
                   WHERE p.JOB_EXECUTION_ID = e.JOB_EXECUTION_ID
                     AND p.PARAMETER_NAME = 'targetDate'
                   LIMIT 1
               ) AS TARGET_DATE
        FROM BATCH_JOB_EXECUTION e
        JOIN BATCH_JOB_INSTANCE i
          ON e.JOB_INSTANCE_ID = i.JOB_INSTANCE_ID
        WHERE i.JOB_NAME = ?
        ORDER BY e.CREATE_TIME DESC, e.JOB_EXECUTION_ID DESC
        LIMIT ?
        """;

    return jdbcTemplate.query(sql, (rs, rowNum) -> mapJobExecution(rs), jobName, safeLimit);
  }

  public List<BatchStepExecutionSummary> findStepExecutions(long jobExecutionId) {
    String sql = """
        SELECT STEP_EXECUTION_ID,
               STEP_NAME,
               STATUS,
               START_TIME,
               END_TIME,
               READ_COUNT,
               WRITE_COUNT,
               READ_SKIP_COUNT,
               PROCESS_SKIP_COUNT,
               WRITE_SKIP_COUNT,
               EXIT_CODE,
               EXIT_MESSAGE
        FROM BATCH_STEP_EXECUTION
        WHERE JOB_EXECUTION_ID = ?
        ORDER BY STEP_EXECUTION_ID ASC
        """;

    return jdbcTemplate.query(sql, (rs, rowNum) -> mapStepExecution(rs), jobExecutionId);
  }

  private BatchJobExecutionSummary mapJobExecution(ResultSet rs) throws SQLException {
    long jobExecutionId = rs.getLong("JOB_EXECUTION_ID");
    LocalDateTime startedAt = toLocalDateTime(rs.getTimestamp("START_TIME"));
    LocalDateTime endedAt = toLocalDateTime(rs.getTimestamp("END_TIME"));

    return new BatchJobExecutionSummary(
        jobExecutionId,
        rs.getString("JOB_NAME"),
        rs.getString("STATUS"),
        startedAt,
        endedAt,
        durationMs(startedAt, endedAt),
        rs.getString("TARGET_DATE"),
        rs.getString("EXIT_CODE"),
        rs.getString("EXIT_MESSAGE"),
        findStepExecutions(jobExecutionId)
    );
  }

  private BatchStepExecutionSummary mapStepExecution(ResultSet rs) throws SQLException {
    LocalDateTime startedAt = toLocalDateTime(rs.getTimestamp("START_TIME"));
    LocalDateTime endedAt = toLocalDateTime(rs.getTimestamp("END_TIME"));
    long skipCount = rs.getLong("READ_SKIP_COUNT")
        + rs.getLong("PROCESS_SKIP_COUNT")
        + rs.getLong("WRITE_SKIP_COUNT");

    return new BatchStepExecutionSummary(
        rs.getLong("STEP_EXECUTION_ID"),
        rs.getString("STEP_NAME"),
        rs.getString("STATUS"),
        startedAt,
        endedAt,
        durationMs(startedAt, endedAt),
        rs.getLong("READ_COUNT"),
        rs.getLong("WRITE_COUNT"),
        skipCount,
        rs.getString("EXIT_CODE"),
        rs.getString("EXIT_MESSAGE")
    );
  }

  private LocalDateTime toLocalDateTime(Timestamp timestamp) {
    return timestamp == null ? null : timestamp.toLocalDateTime();
  }

  private Long durationMs(LocalDateTime startedAt, LocalDateTime endedAt) {
    if (startedAt == null || endedAt == null) {
      return null;
    }
    return Duration.between(startedAt, endedAt).toMillis();
  }
}
