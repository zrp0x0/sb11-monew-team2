package com.codeit.monew.batch.restore.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ArticleRestoreResultResponse(
    LocalDateTime restoreDate,
    List<String> restoredArticleIds,
    long restoredArticleCount
) {

  public static ArticleRestoreResultResponse of(LocalDateTime restoreDate, List<String> restoredArticleIds) {
    return new ArticleRestoreResultResponse(
        restoreDate,
        restoredArticleIds,
        restoredArticleIds.size()
    );
  }
}
