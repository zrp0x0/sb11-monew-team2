package com.codeit.monew.global.batch;

import com.codeit.monew.domain.comment.repository.CommentRepository;
import com.codeit.monew.domain.commentLike.repository.CommentLikeRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class CommentHardDeleteBatchJob {

  private final CommentRepository commentRepository;
  private final CommentLikeRepository commentLikeRepository;

  @Value("${batch.comment.hard-delete.batch-size:1000}")
  private int batchsize;

  @Transactional
  @Scheduled(cron = "0 0 2 * * *") // 매일 새벽 2시
  public void execute() {
    LocalDateTime threshold = LocalDateTime.now().minusDays(7);
    int totalDeleted = 0;

    List<UUID> ids;
    do {
      ids = commentRepository.findIdsByDeletedAtBefore(threshold, batchsize);
      if(!ids.isEmpty()) {
        commentLikeRepository.hardDeleteAllByCommentIdIn(ids);
        commentRepository.hardDeleteAllByIdIn(ids);
        totalDeleted += ids.size();
      }
    } while (ids.size() == batchsize);

    log.info("댓글 물리 삭제 완료. 총 삭제 건수: {}", totalDeleted);
  }
}
