package com.codeit.monew.domain.comment.service;

import com.codeit.monew.domain.article.entity.Article;
import com.codeit.monew.domain.article.repository.ArticleRepository;
import com.codeit.monew.domain.comment.dto.CommentDto;
import com.codeit.monew.domain.comment.dto.CommentOrderBy;
import com.codeit.monew.domain.comment.dto.CommentRegisterRequest;
import com.codeit.monew.domain.comment.dto.CommentUpdateRequest;
import com.codeit.monew.domain.comment.dto.CursorPageResponseCommentDto;
import com.codeit.monew.domain.comment.dto.SortDirection;
import com.codeit.monew.domain.comment.entity.Comment;
import com.codeit.monew.domain.comment.exception.CommentErrorCode;
import com.codeit.monew.domain.comment.exception.CommentException;
import com.codeit.monew.domain.comment.repository.CommentRepository;
import com.codeit.monew.domain.user.entity.User;
import com.codeit.monew.domain.user.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommentService {

  private final CommentRepository commentRepository;
  private final ArticleRepository articleRepository;
  private final UserRepository userRepository;

  @Transactional
  public CommentDto createComment(CommentRegisterRequest request) {
    Article article = articleRepository.findById(request.articleId())
        .orElseThrow(() -> new CommentException(CommentErrorCode.ARTICLE_NOT_FOUND));

    User user = userRepository.findById(request.userId())
        .orElseThrow(() -> new CommentException(CommentErrorCode.USER_NOT_FOUND));

    Comment saved = commentRepository.save(
        Comment.create(article, user, request.content())
    );

    log.info("댓글 등록 성공. CommentId: {}, ArticleId: {}, UserId: {}",
        saved.getId(), request.articleId(), request.userId());

    return CommentDto.of(saved, user.getNickname(), false);
  }

  @Transactional(readOnly = true)
  public CursorPageResponseCommentDto getComments(
      UUID articleId,
      String cursor,
      LocalDateTime after,
      int limit,
      CommentOrderBy orderBy,
      SortDirection direction,
      UUID requestUserId
  ) {
    log.info("댓글 목록 조회. ArticleId: {}, cursor: {}, after: {}, limit: {}, orderBy: {}, direction: {}",
        articleId, cursor, after, limit, orderBy, direction);

    if((cursor == null) != (after == null)) {
      throw new CommentException(CommentErrorCode.INVALID_CURSOR_PARAMETER);
    }

    if(cursor != null) {
      if (orderBy == CommentOrderBy.likeCount) {
        try {
          Integer.parseInt(cursor);
        } catch (NumberFormatException e) {
          throw new CommentException(CommentErrorCode.INVALID_CURSOR_PARAMETER);
        }
      } else {
        try {
          UUID.fromString(cursor);
        } catch (IllegalArgumentException e) {
          throw new CommentException(CommentErrorCode.INVALID_CURSOR_PARAMETER);
        }
      }
    }

    int queryLimit = limit + 1;

    // TODO: 댓글이 많아질 경우 COUNT 쿼리 성능 저하 가능성이 있음
    long totalElements = commentRepository.countByArticleId(articleId);

    UUID cursorId = (cursor != null && orderBy == CommentOrderBy.createdAt)
        ? UUID.fromString(cursor) : null;
    Integer cursorLikeCount = (cursor != null && orderBy == CommentOrderBy.likeCount)
        ? Integer.parseInt(cursor) : null;

    List<Comment> comments = commentRepository.findComments(
        articleId, orderBy, after, cursorId, cursorLikeCount, queryLimit
    );

    List<CommentDto> dtos = comments.stream()
        // TODO: CommentLike 구현 후 requestUserId 기반으로 likedByMe 여부 조회로 교체
        .map(c -> CommentDto.of(c, c.getUser().getNickname(), false))
        .toList();

    return CursorPageResponseCommentDto.of(dtos, limit, totalElements, orderBy);
  }

  @Transactional
  public CommentDto updateComment(String commentId, String userId, CommentUpdateRequest request) {
    UUID commentUUID;
    UUID userUUID;

    if(userId == null || userId.isBlank()) {
      throw new CommentException(CommentErrorCode.MISSING_USER_ID);
    }

    try {
      commentUUID = UUID.fromString(commentId);
      userUUID = UUID.fromString(userId);
    } catch (IllegalArgumentException e) {
      throw new CommentException(CommentErrorCode.INVALID_UUID_FORMAT);
    }
    Comment comment = commentRepository.findById(commentUUID)
        .orElseThrow(() -> new CommentException(CommentErrorCode.COMMENT_NOT_FOUND));

    if(!comment.getUser().getId().equals(userUUID)) {
      throw new CommentException(CommentErrorCode.COMMENT_UNAUTHORIZED);
    }

    comment.update(request.content());

    log.info("댓글 수정 성공. CommentId: {}, UserId: {}", commentUUID, userUUID);

    return CommentDto.of(comment, comment.getUser().getNickname(), false);
  }
}
