package com.codeit.monew.domain.comment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

import com.codeit.monew.domain.article.entity.Article;
import com.codeit.monew.domain.article.repository.ArticleRepository;
import com.codeit.monew.domain.comment.dto.CommentDto;
import com.codeit.monew.domain.comment.dto.CommentRegisterRequest;
import com.codeit.monew.domain.comment.entity.Comment;
import com.codeit.monew.domain.comment.exception.CommentException;
import com.codeit.monew.domain.comment.repository.CommentRepository;
import com.codeit.monew.domain.user.entity.User;
import com.codeit.monew.domain.user.repository.UserRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
