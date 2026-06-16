package com.codeit.monew.batch.delete;

import com.codeit.monew.batch.delete.service.CommentHardDeleteChunkService;
import com.codeit.monew.domain.comment.repository.CommentRepository;
import com.codeit.monew.global.monitoring.domain.JobRunStatus;
import com.codeit.monew.global.monitoring.service.JobRunHistoryCommand;
import com.codeit.monew.global.monitoring.service.JobRunHistoryService;
import com.codeit.monew.global.monitoring.service.MonewMetrics;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CommentHardDeleteBatchJob {

  private static final String JOB_NAME = "commentHardDelete";

  private final CommentRepository commentRepository;
  private final CommentHardDeleteChunkService chunkService;
  private final JobRunHistoryService jobRunHistoryService;
  private final MonewMetrics monewMetrics;

  @Value("${batch.comment.hard-delete.batch-size:1000}")
  private int batchsize;

  @Scheduled(cron = "0 0 2 * * *", zone = "${scheduling.zone:Asia/Seoul}") // 매일 새벽 2시
  public void execute() {
    LocalDateTime startedAt = LocalDateTime.now();
    long startedNanos = System.nanoTime();
    int totalDeleted = 0;
    JobRunStatus status = JobRunStatus.SUCCESS;
    String message = "completed";

    try {
      LocalDateTime threshold = LocalDateTime.now().minusDays(7);

      List<UUID> ids;
      do {
        ids = commentRepository.findIdsByDeletedAtBefore(threshold, batchsize);
        if (!ids.isEmpty()) {
          chunkService.deleteChunk(ids);
          totalDeleted += ids.size();
        }
      } while (ids.size() == batchsize);

      log.info("댓글 물리 삭제 완료. totalDeleted={}", totalDeleted);
    } catch (RuntimeException e) {
      status = JobRunStatus.FAILED;
      message = e.getMessage();
      throw e;
    } finally {
      LocalDateTime endedAt = LocalDateTime.now();
      long durationMs = Duration.ofNanos(System.nanoTime() - startedNanos).toMillis();

      monewMetrics.incrementCommentHardDeleteDeleted(totalDeleted);
      monewMetrics.recordCommentHardDeleteDuration(Duration.ofMillis(durationMs));
      recordRunHistory(status, startedAt, endedAt, durationMs, totalDeleted, message);
    }
  }

  private void recordRunHistory(
      JobRunStatus status,
      LocalDateTime startedAt,
      LocalDateTime endedAt,
      long durationMs,
      int totalDeleted,
      String message
  ) {
    try {
      jobRunHistoryService.record(new JobRunHistoryCommand(
          JOB_NAME,
          status,
          startedAt,
          endedAt,
          durationMs,
          null,
          totalDeleted,
          totalDeleted,
          status == JobRunStatus.FAILED ? 1 : 0,
          0,
          message
      ));
    } catch (Exception e) {
      log.warn("댓글 물리 삭제 실행 이력 저장에 실패했습니다. errorMessage={}", e.getMessage(), e);
    }
  }
}
