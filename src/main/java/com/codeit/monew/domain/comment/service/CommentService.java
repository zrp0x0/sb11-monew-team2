package com.codeit.monew.domain.comment.service;

import com.codeit.monew.domain.article.entity.Article;
import com.codeit.monew.domain.article.repository.ArticleRepository;
import com.codeit.monew.domain.comment.dto.CommentDto;
import com.codeit.monew.domain.comment.dto.CommentRegisterRequest;
import com.codeit.monew.domain.comment.dto.CommentSearchRequest;
import com.codeit.monew.domain.comment.dto.CommentUpdateRequest;
import com.codeit.monew.domain.comment.entity.Comment;
import com.codeit.monew.domain.comment.exception.CommentErrorCode;
import com.codeit.monew.domain.comment.exception.CommentException;
import com.codeit.monew.domain.comment.repository.CommentRepository;
import com.codeit.monew.domain.commentLike.repository.CommentLikeRepository;
import com.codeit.monew.domain.user.entity.User;
import com.codeit.monew.domain.user.repository.UserRepository;
import com.codeit.monew.global.dto.CursorPageResponse;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
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
  private final CommentLikeRepository commentLikeRepository;

  @Transactional
  public CommentDto createComment(CommentRegisterRequest request) {
    Article article = articleRepository.findById(request.articleId())
        .orElseThrow(() -> new CommentException(CommentErrorCode.ARTICLE_NOT_FOUND));

    User user = userRepository.findById(request.userId())
        .orElseThrow(() -> new CommentException(CommentErrorCode.USER_NOT_FOUND));

    Comment saved = commentRepository.save(
        Comment.create(article, user, request.content())
    );

    article.increaseCommentCount();

    log.info("댓글 등록 성공. CommentId: {}, ArticleId: {}, UserId: {}",
        saved.getId(), request.articleId(), request.userId());

    return CommentDto.of(saved, user.getNickname(), false);
  }

  @Transactional(readOnly = true)
  public CursorPageResponse<CommentDto> getComments(
      CommentSearchRequest request,
      UUID requestUserId
  ) {
    log.info("댓글 목록 조회. ArticleId: {}, cursor: {}, after: {}, limit: {}, orderBy: {}, direction: {}",
        request.articleId(), request.cursor(), request.after(), request.getLimit(), request.getOrderBy(), request.getDirection());

    CursorPageResponse<CommentDto> result = commentRepository.findComments(request, requestUserId);

    List<UUID> commentIds = result.content().stream()
        .map(CommentDto::id)
        .collect(Collectors.toList());

    Set<UUID> likedSet = new HashSet<>(
        commentLikeRepository.findByUserIdAndCommentIdIn(requestUserId, commentIds)
    );

    List<CommentDto> updateDtos = result.content().stream()
        .map(dto -> new CommentDto(
            dto.id(),
            dto.articleId(),
            dto.userId(),
            dto.userNickname(),
            dto.content(),
            dto.likeCount(),
            likedSet.contains(dto.id()),
            dto.createdAt()
        ))
        .toList();

    return new CursorPageResponse<>(
        updateDtos,
        result.nextCursor(),
        result.nextAfter(),
        result.size(),
        result.totalElements(),
        result.hasNext()
    );
  }

  @Transactional
  public CommentDto updateComment(UUID commentId, UUID userId, CommentUpdateRequest request) {
    Comment comment = commentRepository.findById(commentId)
        .orElseThrow(() -> new CommentException(CommentErrorCode.COMMENT_NOT_FOUND));

    if(!comment.getUser().getId().equals(userId)) {
      throw new CommentException(CommentErrorCode.COMMENT_UNAUTHORIZED);
    }

    comment.update(request.content());

    log.info("댓글 수정 성공. CommentId: {}, UserId: {}", commentId, userId);

    boolean likedByMe = commentLikeRepository
        .findByCommentIdAndUserId(commentId, userId)
        .isPresent();

    return CommentDto.of(comment, comment.getUser().getNickname(), likedByMe);
  }

  @Transactional
  public void deleteComment(UUID commentId) {
    Comment comment = commentRepository.findById(commentId)
        .orElseThrow(() -> new CommentException(CommentErrorCode.COMMENT_NOT_FOUND));

    comment.softDelete();
    comment.getArticle().decreaseCommentCount();
    log.info("댓글 삭제 성공. CommentId: {}", commentId);
  }

  @Transactional
  public void hardDeleteComment(UUID commentId) {
    Comment comment = commentRepository.findById(commentId)
        .orElseThrow(() -> new CommentException(CommentErrorCode.COMMENT_NOT_FOUND));

    comment.getArticle().decreaseCommentCount();
    commentLikeRepository.deleteAllByCommentId(commentId);
    commentRepository.delete(comment);

    log.info("댓글 물리 삭제 성공. CommentId: {}", commentId);
  }
}
