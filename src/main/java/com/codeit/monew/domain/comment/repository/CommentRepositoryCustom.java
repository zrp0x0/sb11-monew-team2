package com.codeit.monew.domain.comment.repository;

import com.codeit.monew.domain.comment.dto.CommentOrderBy;
import com.codeit.monew.domain.comment.entity.Comment;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface CommentRepositoryCustom {

  List<Comment> findComments(
      UUID articleId,
      CommentOrderBy orderBy,
      LocalDateTime cursorCreatedAt,
      UUID cursorId,
      Integer cursorLikeCount,
      int limit
  );

}
