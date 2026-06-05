package com.codeit.monew.domain.comment.dto;

import java.time.LocalDateTime;
import java.util.List;

public record CursorPageResponseCommentDto(
    List<CommentDto> content,
    String nextCursor,
    LocalDateTime nextAfter,
    int size,
    long totalElements,
    boolean hasNext
) {
  public static CursorPageResponseCommentDto of(
      List<CommentDto> content,
      int size,
      long totalElements,
      CommentOrderBy orderBy
  ) {
    boolean hasNext = content.size() > size;
    List<CommentDto> result = hasNext ? content.subList(0, size) : content;

    String nextCursor = hasNext && orderBy == CommentOrderBy.likeCount
        ? String.valueOf(result.get(result.size() - 1).likeCount())
        : null;
    LocalDateTime nextAfter = hasNext ? result.get(result.size() - 1).createdAt() : null;

    return new CursorPageResponseCommentDto(
        result, nextCursor, nextAfter, result.size(), totalElements, hasNext
    );
  }
}
