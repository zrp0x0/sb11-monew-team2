package com.codeit.monew.domain.comment.dto;

import com.codeit.monew.domain.comment.entity.Comment;
import java.time.LocalDateTime;
import java.util.UUID;

public record CommentDto(
    UUID id,
    UUID articleId,
    UUID userId,
    String userNickname,
    String content,
    long likeCount,
    boolean likedByMe,
    LocalDateTime createdAt
) {
  public static CommentDto of(Comment comment, String userNickname, boolean likedByMe) {
    return new CommentDto(
      comment.getId(),
      comment.getArticle().getId(),
      comment.getUser().getId(),
      userNickname,
      comment.getContent(),
      comment.getLikeCounts(),
      likedByMe,
      comment.getCreatedAt()
    );
  }
}
