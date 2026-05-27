package com.codeit.monew.domain.comment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

import com.codeit.monew.domain.article.entity.Article;
import com.codeit.monew.domain.article.repository.ArticleRepository;
import com.codeit.monew.domain.comment.dto.CommentDto;
import com.codeit.monew.domain.comment.dto.CommentOrderBy;
import com.codeit.monew.domain.comment.dto.CommentRegisterRequest;
import com.codeit.monew.domain.comment.dto.CursorPageResponseCommentDto;
import com.codeit.monew.domain.comment.dto.SortDirection;
import com.codeit.monew.domain.comment.entity.Comment;
import com.codeit.monew.domain.comment.exception.CommentException;
import com.codeit.monew.domain.comment.repository.CommentRepository;
import com.codeit.monew.domain.user.entity.User;
import com.codeit.monew.domain.user.repository.UserRepository;
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
      int limit = 2;
      given(user.getNickname()).willReturn("tester");
      given(user.getId()).willReturn(UUID.randomUUID());
      given(article.getId()).willReturn(articleId);

      List<Comment> comments = List.of(
          Comment.create(article, user, "testComment1"),
          Comment.create(article, user, "testComment2")
      );

      given(commentRepository.countByArticleId(articleId)).willReturn(2L);
      given(commentRepository.findComments(articleId, CommentOrderBy.createdAt, null, null, null, limit + 1)).willReturn(comments);

      CursorPageResponseCommentDto result = commentService.getComments(articleId, null, null, limit,
          CommentOrderBy.createdAt, SortDirection.DESC, UUID.randomUUID());

      assertThat(result.content()).hasSize(2);
      assertThat(result.hasNext()).isFalse();
      assertThat(result.totalElements()).isEqualTo(2L);
      then(commentRepository).should().findComments(articleId, CommentOrderBy.createdAt, null, null, null, limit + 1);
    }

    @Test
    @DisplayName("size + 1개가 조회되면 hasNext가 true이고 content는 size개만 반환")
    void getComments_HasNext_ReturnsOnlySizeItems() {
      int limit = 2;

      Comment comment1 = mock(Comment.class);
      Comment comment2 = mock(Comment.class);
      Comment comment3 = mock(Comment.class);

      given(comment1.getId()).willReturn(UUID.randomUUID());
      given(comment2.getId()).willReturn(UUID.randomUUID());
      given(comment3.getId()).willReturn(UUID.randomUUID());
      given(comment1.getUser()).willReturn(user);
      given(comment2.getUser()).willReturn(user);
      given(comment3.getUser()).willReturn(user);
      given(comment1.getArticle()).willReturn(article);
      given(comment2.getArticle()).willReturn(article);
      given(comment3.getArticle()).willReturn(article);
      given(comment1.getCreatedAt()).willReturn(LocalDateTime.now());
      given(comment2.getCreatedAt()).willReturn(LocalDateTime.now());
      given(comment3.getCreatedAt()).willReturn(LocalDateTime.now());
      given(user.getNickname()).willReturn("tester");
      given(user.getId()).willReturn(UUID.randomUUID());
      given(article.getId()).willReturn(articleId);

      List<Comment> comments = List.of(comment1, comment2, comment3);

      given(commentRepository.countByArticleId(articleId)).willReturn(3L);
      given(commentRepository.findComments(articleId, CommentOrderBy.createdAt, null, null, null, limit + 1))
          .willReturn(comments);

      CursorPageResponseCommentDto result = commentService.getComments(
          articleId, null, null, limit, CommentOrderBy.createdAt, SortDirection.DESC, UUID.randomUUID());

      assertThat(result.content()).hasSize(limit);
      assertThat(result.hasNext()).isTrue();
      assertThat(result.nextCursor()).isNotNull();
      assertThat(result.nextAfter()).isNotNull();
    }

    @Test
    @DisplayName("cursor와 after가 있으면 커서 기반 다음 페이지를 조회")
    void getComments_WithCursor_ReturnsNextPage() {
      int limit = 2;
      String cursor = UUID.randomUUID().toString();
      LocalDateTime after = LocalDateTime.now();

      Comment comment1 = mock(Comment.class);
      Comment comment2 = mock(Comment.class);

      given(comment1.getId()).willReturn(UUID.randomUUID());
      given(comment2.getId()).willReturn(UUID.randomUUID());
      given(comment1.getUser()).willReturn(user);
      given(comment2.getUser()).willReturn(user);
      given(comment1.getArticle()).willReturn(article);
      given(comment2.getArticle()).willReturn(article);
      given(comment1.getCreatedAt()).willReturn(LocalDateTime.now());
      given(comment2.getCreatedAt()).willReturn(LocalDateTime.now());
      given(user.getNickname()).willReturn("tester");
      given(user.getId()).willReturn(UUID.randomUUID());
      given(article.getId()).willReturn(articleId);

      List<Comment> comments = List.of(comment1, comment2);

      given(commentRepository.countByArticleId(articleId)).willReturn(4L);
      given(commentRepository.findComments(
          eq(articleId), eq(CommentOrderBy.createdAt), eq(after), any(UUID.class), isNull(), eq(limit + 1)))
          .willReturn(comments);

      CursorPageResponseCommentDto result = commentService.getComments(
          articleId, cursor, after, limit, CommentOrderBy.createdAt, SortDirection.DESC, UUID.randomUUID());

      assertThat(result.content()).hasSize(2);
      assertThat(result.hasNext()).isFalse();
      then(commentRepository).should().findComments(eq(articleId), eq(CommentOrderBy.createdAt), eq(after), any(UUID.class), isNull(), eq(limit + 1));
    }

    @Test
    @DisplayName("cursor만 있고 after가 없으면 CommentException을 던짐")
    void getComments_OnlyCursor_ThrowsCommentException() {
      String cursor = UUID.randomUUID().toString();

      assertThatThrownBy(() ->
          commentService.getComments(articleId, cursor, null, 10,
              CommentOrderBy.createdAt, SortDirection.DESC, UUID.randomUUID()))
          .isInstanceOf(CommentException.class);

      then(commentRepository).should(never()).findComments(any(), any(), any(), any(), any(), anyInt());
    }

    @Test
    @DisplayName("after만 있고 cursor가 없으면 CommentException을 던짐")
    void getComments_OnlyAfter_ThrowsCommentException() {
      LocalDateTime after = LocalDateTime.now();

      assertThatThrownBy(() ->
          commentService.getComments(articleId, null, after, 10,
              CommentOrderBy.createdAt, SortDirection.DESC, UUID.randomUUID()))
          .isInstanceOf(CommentException.class);
    }

    @Test
    @DisplayName("cursor가 UUID형식이 아닐 경우 CommentException을 던짐")
    void getComments_InvalidCursorFormat_ThrowsCommentException() {
      String invalidCursor = "invalid-uuid";
      LocalDateTime after = LocalDateTime.now();

      assertThatThrownBy(() ->
          commentService.getComments(articleId, invalidCursor, after, 10,
              CommentOrderBy.createdAt, SortDirection.DESC, UUID.randomUUID()))
          .isInstanceOf(CommentException.class);
    }

    @Test
    @DisplayName("likeCount 정렬 시 첫 페이지를 조회")
    void getComments_likeCountFirstPage_ReturnsCursorPageResponse() {
      int limit = 2;
      given(user.getNickname()).willReturn("tester");
      given(user.getId()).willReturn(UUID.randomUUID());
      given(article.getId()).willReturn(articleId);

      List<Comment> comments = List.of(
          Comment.create(article, user, "testComment1"),
          Comment.create(article, user, "testComment2")
      );

      given(commentRepository.countByArticleId(articleId)).willReturn(2L);
      given(commentRepository.findComments(articleId, CommentOrderBy.likeCount, null, null, null, limit + 1))
          .willReturn(comments);

      CursorPageResponseCommentDto result = commentService.getComments(
          articleId, null, null, limit, CommentOrderBy.likeCount, SortDirection.DESC, UUID.randomUUID());

      assertThat(result.content()).hasSize(2);
      assertThat(result.hasNext()).isFalse();
      then(commentRepository).should()
          .findComments(articleId, CommentOrderBy.likeCount, null, null, null, limit + 1);
    }

    @Test
    @DisplayName("likeCount 정렬 시 cursor와 after가 있으면 다음 페이지를 조회한다")
    void getComments_LikeCountWithCursor_ReturnsNextPage() {
      int limit = 2;
      String cursor = "10";
      LocalDateTime after = LocalDateTime.now();

      Comment comment1 = mock(Comment.class);
      Comment comment2 = mock(Comment.class);

      given(comment1.getId()).willReturn(UUID.randomUUID());
      given(comment2.getId()).willReturn(UUID.randomUUID());
      given(comment1.getUser()).willReturn(user);
      given(comment2.getUser()).willReturn(user);
      given(comment1.getArticle()).willReturn(article);
      given(comment2.getArticle()).willReturn(article);
      given(comment1.getCreatedAt()).willReturn(LocalDateTime.now());
      given(comment2.getCreatedAt()).willReturn(LocalDateTime.now());
      given(user.getNickname()).willReturn("tester");
      given(user.getId()).willReturn(UUID.randomUUID());
      given(article.getId()).willReturn(articleId);

      List<Comment> comments = List.of(comment1, comment2);

      given(commentRepository.countByArticleId(articleId)).willReturn(4L);
      given(commentRepository.findComments(
          eq(articleId), eq(CommentOrderBy.likeCount), any(LocalDateTime.class), isNull(), eq(10), eq(limit + 1)))
          .willReturn(comments);

      CursorPageResponseCommentDto result = commentService.getComments(
          articleId, cursor, after, limit, CommentOrderBy.likeCount, SortDirection.DESC, UUID.randomUUID());

      assertThat(result.content()).hasSize(2);
      assertThat(result.hasNext()).isFalse();
      then(commentRepository).should()
          .findComments(eq(articleId), eq(CommentOrderBy.likeCount), any(LocalDateTime.class), isNull(), eq(10), eq(limit + 1));
    }
  }
}
