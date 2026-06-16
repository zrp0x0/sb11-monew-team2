package com.codeit.monew.global.monitoring.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MonewMetrics {

  public static final String NEWS_COLLECT_DURATION = "monew.news.collect.duration";
  public static final String NEWS_COLLECT_CANDIDATES = "monew.news.collect.candidates";
  public static final String NEWS_COLLECT_SAVED = "monew.news.collect.saved";
  public static final String NEWS_COLLECT_FAILED = "monew.news.collect.failed";
  public static final String NEWS_COLLECT_DUPLICATES = "monew.news.collect.duplicates";
  public static final String NAVER_CALLS = "monew.external.naver.calls";
  public static final String NAVER_ERRORS = "monew.external.naver.errors";
  public static final String NAVER_EMPTY_RESPONSES = "monew.external.naver.empty_responses";
  public static final String NAVER_DURATION = "monew.external.naver.duration";
  public static final String COMMENT_HARD_DELETE_DELETED = "monew.comment.hard_delete.deleted";
  public static final String COMMENT_HARD_DELETE_DURATION = "monew.comment.hard_delete.duration";
  public static final String ARTICLE_BACKUP_SUCCESS = "monew.batch.article_backup.success";
  public static final String ARTICLE_BACKUP_FAILURE = "monew.batch.article_backup.failure";
  public static final String ARTICLE_BACKUP_DURATION = "monew.batch.article_backup.duration";

  private final MeterRegistry meterRegistry;

  public void incrementNewsCollectCandidates(long count) {
    increment(NEWS_COLLECT_CANDIDATES, count);
  }

  public void incrementNewsCollectSaved(long count) {
    increment(NEWS_COLLECT_SAVED, count);
  }

  public void incrementNewsCollectFailed(long count) {
    increment(NEWS_COLLECT_FAILED, count);
  }

  public void incrementNewsCollectDuplicates(long count) {
    increment(NEWS_COLLECT_DUPLICATES, count);
  }

  public void recordNewsCollectDuration(Duration duration) {
    recordDuration(NEWS_COLLECT_DURATION, duration);
  }

  public void incrementNaverCalls() {
    increment(NAVER_CALLS, 1);
  }

  public void incrementNaverErrors() {
    increment(NAVER_ERRORS, 1);
  }

  public void incrementNaverEmptyResponses() {
    increment(NAVER_EMPTY_RESPONSES, 1);
  }

  public void recordNaverDuration(Duration duration) {
    recordDuration(NAVER_DURATION, duration);
  }

  public void incrementCommentHardDeleteDeleted(long count) {
    increment(COMMENT_HARD_DELETE_DELETED, count);
  }

  public void recordCommentHardDeleteDuration(Duration duration) {
    recordDuration(COMMENT_HARD_DELETE_DURATION, duration);
  }

  public void incrementArticleBackupSuccess() {
    increment(ARTICLE_BACKUP_SUCCESS, 1);
  }

  public void incrementArticleBackupFailure() {
    increment(ARTICLE_BACKUP_FAILURE, 1);
  }

  public void recordArticleBackupDuration(Duration duration) {
    recordDuration(ARTICLE_BACKUP_DURATION, duration);
  }

  private void increment(String name, long count) {
    if (count <= 0) {
      return;
    }

    Counter.builder(name).register(meterRegistry).increment(count);
  }

  private void recordDuration(String name, Duration duration) {
    if (duration == null || duration.isNegative()) {
      return;
    }

    Timer.builder(name).register(meterRegistry).record(duration);
  }
}
