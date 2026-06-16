package com.codeit.monew.batch.collector.provider;

import com.codeit.monew.domain.article.entity.ArticleSource;
import java.util.Collections;
import java.util.List;

public record NewsFetchResult(
    ArticleSource source,
    NewsFetchStatus status,
    List<CollectedNewsDto> items,
    String message,
    boolean apiCalled
) {

  public NewsFetchResult {
    items = items == null ? Collections.emptyList() : List.copyOf(items);
  }

  public static NewsFetchResult success(ArticleSource source, List<CollectedNewsDto> items) {
    return new NewsFetchResult(source, NewsFetchStatus.SUCCESS, items, null, true);
  }

  public static NewsFetchResult empty(ArticleSource source, String message) {
    return new NewsFetchResult(source, NewsFetchStatus.EMPTY_RESPONSE, Collections.emptyList(), message, true);
  }

  public static NewsFetchResult failed(ArticleSource source, String message) {
    return new NewsFetchResult(source, NewsFetchStatus.FAILED, Collections.emptyList(), message, true);
  }

  public static NewsFetchResult skipped(ArticleSource source, String message) {
    return new NewsFetchResult(source, NewsFetchStatus.SKIPPED, Collections.emptyList(), message, false);
  }
}
