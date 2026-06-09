package com.codeit.monew.global;

import com.codeit.monew.batch.backup.scheduler.ArticleBackupScheduler;
import com.codeit.monew.batch.collector.service.NewsCollectorService;
import com.codeit.monew.global.batch.CommentHardDeleteBatchJob;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/test")
public class TestController {

  private final NewsCollectorService newsCollectorService;
  private final ArticleBackupScheduler articleBackupScheduler;
  private final CommentHardDeleteBatchJob commentHardDeleteBatchJob;

  @GetMapping()
  public String testGetMapping() {
    log.info("Hello World!");
    return "Hello World!";
  }

  @Operation(summary = "수동 뉴스 수집 작업", description = "수동으로 뉴스 수집 작업을 돌립니다.")
  @GetMapping("/news-batch")
  public void newsBatch() {
    newsCollectorService.collectNewsHourly();
  }

  @Operation(summary = "수동 뉴스 백업 작업", description = "수동으로 뉴스 백업 작업을 돌립니다.")
  @GetMapping("/news-backup")
  public void newsBackup() {
    articleBackupScheduler.runArticleBackupJob();
  }

  @Operation(summary = "수동 댓글 물리 삭제 작업", description = "수동으로 댓글 물리 삭제를 돌립니다.")
  @GetMapping("/comment-hard-delete")
  public void hardDeleteComment() {
    commentHardDeleteBatchJob.execute();
  }
}
