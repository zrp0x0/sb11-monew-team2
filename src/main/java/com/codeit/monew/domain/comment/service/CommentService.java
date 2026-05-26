package com.codeit.monew.domain.comment.service;

import com.codeit.monew.domain.article.entity.Article;
import com.codeit.monew.domain.article.repository.ArticleRepository;
import com.codeit.monew.domain.comment.dto.CommentDto;
import com.codeit.monew.domain.comment.dto.CommentRegisterRequest;
import com.codeit.monew.domain.comment.dto.CursorPageResponseCommentDto;
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
      int size
  ) {
    log.info("댓글 목록 조회. ArticleId: {}, cursor: {}, after: {}, size: {}",
        articleId, cursor, after, size);

    if((cursor == null) != (after == null)) {
      throw new CommentException(CommentErrorCode.INVALID_CURSOR_PARAMETER);
    }

    if(cursor != null) {
      try {
        UUID.fromString(cursor);
      } catch (IllegalArgumentException e) {
        throw new CommentException(CommentErrorCode.INVALID_CURSOR_PARAMETER);
      }
    }

    int limit = size + 1;

    // TODO: 댓글이 많아질 경우 COUNT 쿼리 성능 저하 가능성이 있음
    long totalElements = commentRepository.countByArticleId(articleId);

    List<Comment> comments = (cursor == null || after == null)
        ? commentRepository.findByArticleIdFirstPage(articleId, limit)
        : commentRepository.findByArticleIdAfterCursor(
            articleId, after, UUID.fromString(cursor), limit
        );

    List<CommentDto> dtos = comments.stream()
        .map(c -> CommentDto.of(c, c.getUser().getNickname(), false))
        .toList();

    return CursorPageResponseCommentDto.of(dtos, size, totalElements);
  }
}
