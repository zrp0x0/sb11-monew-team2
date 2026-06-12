package com.codeit.monew.domain.comment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

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
import com.codeit.monew.domain.commentLike.entity.CommentLike;
import com.codeit.monew.domain.commentLike.repository.CommentLikeRepository;
import com.codeit.monew.domain.user.entity.User;
import com.codeit.monew.domain.user.repository.UserRepository;
import com.codeit.monew.global.dto.CursorPageResponse;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CommentServiceTest {

  @InjectMocks
  private CommentService commentService;

  @Mock
  private CommentRepository commentRepository;

  @Mock
  private ArticleRepository articleRepository;

  @Mock
  private UserRepository userRepository;

  @Mock
  private CommentLikeRepository commentLikeRepository;

  private UUID articleId;
  private UUID userId;
  private Article article;
  private User user;

  @BeforeEach
  void setUp() {
    articleId = UUID.randomUUID();
    userId = UUID.randomUUID();
    article = mock(Article.class);
    user = mock(User.class);
  }

  @Nested
  @DisplayName("createComment")
  class CreateComment {

    @Test
    @DisplayName("유효한 요청일 경우 댓글을 저장하고 CommentDto를 반환")
    void createComment_ValidRequest_SavesAndReturnsDto() {
      CommentRegisterRequest request = new CommentRegisterRequest(articleId, userId, "test");

      Comment comment = Comment.create(article, user, "test");

      given(articleRepository.findById(articleId)).willReturn(Optional.of(article));
      given(userRepository.findById(userId)).willReturn(Optional.of(user));
      given(commentRepository.save(any(Comment.class))).willReturn(comment);
      given(user.getNickname()).willReturn("tester");
      given(user.getId()).willReturn(userId);
      given(article.getId()).willReturn(articleId);

      CommentDto result = commentService.createComment(request);

      assertThat(result.content()).isEqualTo("test");
      assertThat(result.userNickname()).isEqualTo("tester");
      assertThat(result.likedByMe()).isFalse();
      assertThat(result.likeCount()).isZero();
      then(commentRepository).should().save(any(Comment.class));
      then(article).should().increaseCommentCount();
    }

    @Test
    @DisplayName("존재하지 않는 Article이면 CommentException을 던지고 저장하지 않음")
    void createComment_ArticleNotFound_ThrowsCommentException() {
      CommentRegisterRequest request = new CommentRegisterRequest(articleId, userId, "testComment");
      given(articleRepository.findById(articleId)).willReturn(Optional.empty());

      assertThatThrownBy(() -> commentService.createComment(request))
          .isInstanceOf(CommentException.class);

      then(commentRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("존재하지 않는 User이면 CommentException을 던지고 저장하지 않음")
    void createComment_UserNotFound_ThrowsCommentException() {
      CommentRegisterRequest request = new CommentRegisterRequest(articleId, userId, "testComment");
      given(articleRepository.findById(articleId)).willReturn(Optional.of(article));
      given(userRepository.findById(userId)).willReturn(Optional.empty());

      assertThatThrownBy(() -> commentService.createComment(request))
          .isInstanceOf(CommentException.class);

      then(commentRepository).should(never()).save(any());
    }
  }

  @Nested
  @DisplayName("getComments")
  class GetComments {

    @Test
    @DisplayName("cursor 없이 첫 페이지 조회 시 CursorPageResponseCommentDto를 반환")
    void getComments_FirstPage_ReturnsCursorPageResponse() {
      UUID requestUserId = UUID.randomUUID();

      CommentSearchRequest request = new CommentSearchRequest(
          articleId, null, null, 2, "createdAt", "DESC"
      );

      List<CommentDto> commentDtos = List.of(
          new CommentDto(UUID.randomUUID(), articleId, UUID.randomUUID(), "tester", "testComment1", 0, false, LocalDateTime.now()),
          new CommentDto(UUID.randomUUID(), articleId, UUID.randomUUID(), "tester", "testComment2", 0, false, LocalDateTime.now())
      );

      CursorPageResponse<CommentDto> repoResult = new CursorPageResponse<>(
          commentDtos, null, null, 2, 2L, false
      );

      given(commentRepository.findComments(any(CommentSearchRequest.class), eq(requestUserId)))
          .willReturn(repoResult);
      given(commentLikeRepository.findByUserIdAndCommentIdIn(eq(requestUserId), anyList()))
          .willReturn(List.of());

      CursorPageResponse<CommentDto> result = commentService.getComments(request, requestUserId);

      assertThat(result.content()).hasSize(2);
      assertThat(result.hasNext()).isFalse();
      assertThat(result.totalElements()).isEqualTo(2L);
      then(commentRepository).should().findComments(any(CommentSearchRequest.class), eq(requestUserId));
    }

    @Test
    @DisplayName("hasNext가 true이면 nextCursor와 nextAfter가 존재")
    void getComments_HasNext_ReturnsNextCursor() {
      UUID requestUserId = UUID.randomUUID();

      CommentSearchRequest request = new CommentSearchRequest(
          articleId, null, null, 2, "createdAt", "DESC"
      );

      List<CommentDto> commentDtos = List.of(
          new CommentDto(UUID.randomUUID(), articleId, UUID.randomUUID(), "tester", "comment1", 0, false, LocalDateTime.now()),
          new CommentDto(UUID.randomUUID(), articleId, UUID.randomUUID(), "tester", "comment2", 0, false, LocalDateTime.now())
      );

      CursorPageResponse<CommentDto> repoResult = new CursorPageResponse<>(
          commentDtos, "nextCursorValue", "2026-06-05T13:49:35.781650", 2, 3L, true
      );

      given(commentRepository.findComments(any(CommentSearchRequest.class), eq(requestUserId)))
          .willReturn(repoResult);
      given(commentLikeRepository.findByUserIdAndCommentIdIn(eq(requestUserId), anyList()))
          .willReturn(List.of());

      CursorPageResponse<CommentDto> result = commentService.getComments(request, requestUserId);

      assertThat(result.content()).hasSize(2);
      assertThat(result.hasNext()).isTrue();
      assertThat(result.nextCursor()).isNotNull();
      assertThat(result.nextAfter()).isNotNull();
    }

    @Test
    @DisplayName("cursor가 있으면 다음 페이지를 조회")
    void getComments_WithCursor_ReturnsNextPage() {
      UUID requestUserId = UUID.randomUUID();
      String cursor = UUID.randomUUID().toString();
      LocalDateTime after = LocalDateTime.now();

      CommentSearchRequest request = new CommentSearchRequest(
          articleId, cursor, after, 2, "createdAt", "DESC"
      );

      List<CommentDto> commentDtos = List.of(
          new CommentDto(UUID.randomUUID(), articleId, UUID.randomUUID(), "tester", "comment1", 0, false, LocalDateTime.now()),
          new CommentDto(UUID.randomUUID(), articleId, UUID.randomUUID(), "tester", "comment2", 0, false, LocalDateTime.now())
      );

      CursorPageResponse<CommentDto> repoResult = new CursorPageResponse<>(
          commentDtos, null, null, 2, null, false
      );

      given(commentRepository.findComments(any(CommentSearchRequest.class), eq(requestUserId)))
          .willReturn(repoResult);
      given(commentLikeRepository.findByUserIdAndCommentIdIn(eq(requestUserId), anyList()))
          .willReturn(List.of());

      CursorPageResponse<CommentDto> result = commentService.getComments(request, requestUserId);

      assertThat(result.content()).hasSize(2);
      assertThat(result.hasNext()).isFalse();
      then(commentRepository).should().findComments(any(CommentSearchRequest.class), eq(requestUserId));
    }

    @Test
    @DisplayName("좋아요를 누른 댓글은 likedByMe가 true로 반환")
    void getComments_LikedComment_ReturnsLikedByMeTrue() {
      UUID requestUserId = UUID.randomUUID();
      UUID comment1Id = UUID.randomUUID();
      UUID comment2Id = UUID.randomUUID();

      CommentSearchRequest request = new CommentSearchRequest(
          articleId, null, null, 2, "createdAt", "DESC"
      );

      List<CommentDto> commentDtos = List.of(
          new CommentDto(comment1Id, articleId, UUID.randomUUID(), "tester", "comment1", 0, false, LocalDateTime.now()),
          new CommentDto(comment2Id, articleId, UUID.randomUUID(), "tester", "comment2", 0, false, LocalDateTime.now())
      );

      CursorPageResponse<CommentDto> repoResult = new CursorPageResponse<>(
          commentDtos, null, null, 2, 2L, false
      );

      given(commentRepository.findComments(any(CommentSearchRequest.class), eq(requestUserId)))
          .willReturn(repoResult);
      given(commentLikeRepository.findByUserIdAndCommentIdIn(eq(requestUserId), anyList()))
          .willReturn(List.of(comment1Id));

      CursorPageResponse<CommentDto> result = commentService.getComments(request, requestUserId);

      assertThat(result.content().get(0).likedByMe()).isTrue();
      assertThat(result.content().get(1).likedByMe()).isFalse();
    }

    @Test
    @DisplayName("LIKE_COUNT 정렬 시 첫 페이지를 조회")
    void getComments_likeCountFirstPage_ReturnsCursorPageResponse() {
      UUID requestUserId = UUID.randomUUID();

      CommentSearchRequest request = new CommentSearchRequest(
          articleId, null, null, 2, "likeCount", "DESC"
      );

      List<CommentDto> commentDtos = List.of(
          new CommentDto(UUID.randomUUID(), articleId, UUID.randomUUID(), "tester", "comment1", 3, false, LocalDateTime.now()),
          new CommentDto(UUID.randomUUID(), articleId, UUID.randomUUID(), "tester", "comment2", 1, false, LocalDateTime.now())
      );

      CursorPageResponse<CommentDto> repoResult = new CursorPageResponse<>(
          commentDtos, null, null, 2, 2L, false
      );

      given(commentRepository.findComments(any(CommentSearchRequest.class), eq(requestUserId)))
          .willReturn(repoResult);
      given(commentLikeRepository.findByUserIdAndCommentIdIn(eq(requestUserId), anyList()))
          .willReturn(List.of());

      CursorPageResponse<CommentDto> result = commentService.getComments(request, requestUserId);

      assertThat(result.content()).hasSize(2);
      assertThat(result.hasNext()).isFalse();
      then(commentRepository).should()
          .findComments(any(CommentSearchRequest.class), eq(requestUserId));
    }
  }

  @Nested
  @DisplayName("updateComment")
  class UpdateComment {

    @Test
    @DisplayName("유효한 요청일 경우 댓글을 수정하고 CommentDto를 반환")
    void updateComment_ValidRequest_UpdateAndReturnsDto() {
      UUID commentId = UUID.randomUUID();
      CommentUpdateRequest request = new CommentUpdateRequest("updateTest");

      Comment comment = mock(Comment.class);

      given(commentRepository.findById(commentId)).willReturn(Optional.of(comment));
      given(comment.getUser()).willReturn(user);
      given(comment.getArticle()).willReturn(article);
      given(comment.getId()).willReturn(commentId);
      given(comment.getContent()).willReturn("updateTest");
      given(user.getId()).willReturn(userId);
      given(user.getNickname()).willReturn("tester");
      given(article.getId()).willReturn(articleId);
      given(commentLikeRepository.findByCommentIdAndUserId(any(UUID.class), any(UUID.class)))
          .willReturn(Optional.empty());

      CommentDto result = commentService.updateComment(commentId, userId, request);

      assertThat(result.content()).isEqualTo("updateTest");
      assertThat(result.userNickname()).isEqualTo("tester");
      assertThat(result.likedByMe()).isFalse();
    }

    @Test
    @DisplayName("좋아요를 누른 댓글 수정 시 likedByMe가 true로 반환")
    void updateComment_LikedComment_ReturnsLikedByMeTrue() {
      UUID commentId = UUID.randomUUID();
      CommentUpdateRequest request = new CommentUpdateRequest("updateTest");

      Comment comment = mock(Comment.class);

      given(commentRepository.findById(commentId)).willReturn(Optional.of(comment));
      given(comment.getUser()).willReturn(user);
      given(comment.getArticle()).willReturn(article);
      given(comment.getId()).willReturn(commentId);
      given(user.getId()).willReturn(userId);
      given(user.getNickname()).willReturn("tester");
      given(article.getId()).willReturn(articleId);
      given(commentLikeRepository.findByCommentIdAndUserId(any(UUID.class), any(UUID.class)))
          .willReturn(Optional.of(mock(CommentLike.class)));

      CommentDto result = commentService.updateComment(commentId, userId, request);

      assertThat(result.likedByMe()).isTrue();
    }

    @Test
    @DisplayName("존재하지 않는 commentId이면 CommentException을 던짐")
    void updateComment_CommentNotFound_ThrowsCommentException() {
      UUID commentId = UUID.randomUUID();
      CommentUpdateRequest request = new CommentUpdateRequest("updateTest");

      given(commentRepository.findById(commentId)).willReturn(Optional.empty());

      assertThatThrownBy(() -> commentService.updateComment(commentId, userId, request))
          .isInstanceOf(CommentException.class);
    }

    @Test
    @DisplayName("댓글 작성자가 아닐 경우 CommentException을 던짐")
    void updateComment_IsNotCommentOwner_ThrowsCommentException() {
      UUID commentId = UUID.randomUUID();
      UUID otherUserId = UUID.randomUUID();
      CommentUpdateRequest request = new CommentUpdateRequest("updateTest");

      Comment comment = Comment.create(article, user, "test");

      given(commentRepository.findById(commentId)).willReturn(Optional.of(comment));
      given(user.getId()).willReturn(userId);

      assertThatThrownBy(() -> commentService.updateComment(commentId, otherUserId, request))
          .isInstanceOf(CommentException.class)
          .extracting("errorCode")
          .isEqualTo(CommentErrorCode.COMMENT_UNAUTHORIZED);
    }
  }

  @Nested
  @DisplayName("deleteComment")
  class DeleteComment {

    @Test
    @DisplayName("유효한 요청일 경우 댓글을 논리 삭제")
    void deleteComment_ValidRequest_SoftDeletesComment() {
      UUID commentId = UUID.randomUUID();

      Comment comment = mock(Comment.class);

      given(commentRepository.findById(commentId)).willReturn(Optional.of(comment));
      given(comment.getArticle()).willReturn(article);

      commentService.deleteComment(commentId);

      then(comment).should().softDelete();
      then(article).should().decreaseCommentCount();
    }

    @Test
    @DisplayName("존재하지 않는 commentId이면 CommentException을 던짐")
    void deleteComment_CommentNotFound_ThrowsCommentException() {
      UUID commentId = UUID.randomUUID();

      given(commentRepository.findById(commentId)).willReturn(Optional.empty());

      assertThatThrownBy(() -> commentService.deleteComment(commentId))
          .isInstanceOf(CommentException.class)
          .extracting("errorCode")
          .isEqualTo(CommentErrorCode.COMMENT_NOT_FOUND);
    }
  }

  @Nested
  @DisplayName("hardDeleteComment")
  class HardDeleteComment {

    @Test
    @DisplayName("유효한 요청일 경우 CommentLike를 먼저 물리 삭제 후 댓글 물리 삭제")
    void hardDeleteComment_ValidRequest_DeletesCommentAndLikes() {
      UUID commentId = UUID.randomUUID();

      Comment comment = mock(Comment.class);

      given(commentRepository.findById(commentId)).willReturn(Optional.of(comment));
      given(comment.getArticle()).willReturn(article);

      commentService.hardDeleteComment(commentId);

      then(commentLikeRepository).should().deleteAllByCommentId(commentId);
      then(commentRepository).should().delete(comment);
      then(article).should().decreaseCommentCount();
    }

    @Test
    @DisplayName("존재하지 않는 commentId이면 CommentException을 던짐")
    void hardDeleteComment_CommentNotFound_ThrowsCommentException() {
      UUID commentId = UUID.randomUUID();

      given(commentRepository.findById(commentId)).willReturn(Optional.empty());

      assertThatThrownBy(() -> commentService.hardDeleteComment(commentId))
          .isInstanceOf(CommentException.class)
          .extracting("errorCode")
          .isEqualTo(CommentErrorCode.COMMENT_NOT_FOUND);

      then(commentLikeRepository).should(never()).deleteAllByCommentId(any());
      then(commentRepository).should(never()).delete(any());
    }
  }
}
