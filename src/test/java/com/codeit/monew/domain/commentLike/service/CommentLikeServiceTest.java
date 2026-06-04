package com.codeit.monew.domain.commentLike.service;

import com.codeit.monew.domain.article.entity.Article;
import com.codeit.monew.domain.comment.entity.Comment;
import com.codeit.monew.domain.comment.repository.CommentRepository;
import com.codeit.monew.domain.commentLike.entity.CommentLike;
import com.codeit.monew.domain.commentLike.exception.CommentLikeException;
import com.codeit.monew.domain.commentLike.repository.CommentLikeRepository;
import com.codeit.monew.domain.notification.listener.NotificationCreateEvent;
import com.codeit.monew.domain.user.entity.User;
import com.codeit.monew.domain.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommentLikeServiceTest {

    @Mock
    private CommentLikeRepository commentLikeRepository;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private CommentLikeService commentLikeService;

    @Test
    void create_ShouldSucceed() {
        UUID commentId = UUID.randomUUID();
        UUID requestUserId = UUID.randomUUID();
        UUID articleId = UUID.randomUUID();
        UUID commentUserId = UUID.randomUUID();

        Comment comment = mock(Comment.class);
        User likedUser = mock(User.class);
        User commentUser = mock(User.class);
        Article article = mock(Article.class);

        when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));
        when(userRepository.findById(requestUserId)).thenReturn(Optional.of(likedUser));
        when(commentLikeRepository.findByCommentIdAndUserId(commentId, requestUserId))
                .thenReturn(Optional.empty());

        when(commentLikeRepository.save(any(CommentLike.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(comment.getArticle()).thenReturn(article);
        when(comment.getUser()).thenReturn(commentUser);

        when(article.getId()).thenReturn(articleId);
        when(likedUser.getId()).thenReturn(requestUserId);

        when(comment.getId()).thenReturn(commentId);
        when(commentUser.getId()).thenReturn(commentUserId);
        when(commentUser.getNickname()).thenReturn("nickname");

        when(comment.getContent()).thenReturn("content");
        when(comment.getLikeCounts()).thenReturn(1);

        commentLikeService.create(commentId, requestUserId);

        verify(commentLikeRepository).save(any(CommentLike.class));
        verify(comment).increaseLikeCount();
        verify(eventPublisher).publishEvent(any(NotificationCreateEvent.class));
    }

    @Test
    void create_ShouldThrow_WhenCommentDoesNotExist() {
        UUID commentId = UUID.randomUUID();
        UUID requestUserId = UUID.randomUUID();

        when(commentRepository.findById(commentId))
                .thenReturn(Optional.empty());

        assertThrows(CommentLikeException.class,
                () -> commentLikeService.create(commentId, requestUserId));

        verify(userRepository, never()).findById(any());
        verify(commentLikeRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void create_ShouldThrow_WhenUserDoesNotExist() {
        UUID commentId = UUID.randomUUID();
        UUID requestUserId = UUID.randomUUID();

        Comment comment = mock(Comment.class);

        when(commentRepository.findById(commentId))
                .thenReturn(Optional.of(comment));

        when(userRepository.findById(requestUserId))
                .thenReturn(Optional.empty());

        assertThrows(CommentLikeException.class,
                () -> commentLikeService.create(commentId, requestUserId));

        verify(commentLikeRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void create_ShouldThrow_WhenCommentLikeAlreadyExists() {
        UUID commentId = UUID.randomUUID();
        UUID requestUserId = UUID.randomUUID();

        Comment comment = mock(Comment.class);
        User user = mock(User.class);
        CommentLike commentLike = mock(CommentLike.class);

        when(commentRepository.findById(commentId))
                .thenReturn(Optional.of(comment));

        when(userRepository.findById(requestUserId))
                .thenReturn(Optional.of(user));

        when(commentLikeRepository.findByCommentIdAndUserId(commentId, requestUserId))
                .thenReturn(Optional.of(commentLike));

        assertThrows(CommentLikeException.class,
                () -> commentLikeService.create(commentId, requestUserId));

        verify(commentLikeRepository, never()).save(any());
        verify(comment, never()).increaseLikeCount();
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void delete_ShouldSucceed() {
        UUID commentId = UUID.randomUUID();
        UUID requestUserId = UUID.randomUUID();

        Comment comment = mock(Comment.class);
        User user = mock(User.class);
        CommentLike commentLike = mock(CommentLike.class);

        when(commentRepository.findById(commentId))
                .thenReturn(Optional.of(comment));

        when(userRepository.findById(requestUserId))
                .thenReturn(Optional.of(user));

        when(commentLikeRepository.findByCommentIdAndUserId(commentId, requestUserId))
                .thenReturn(Optional.of(commentLike));

        commentLikeService.delete(commentId, requestUserId);

        verify(commentLikeRepository).delete(commentLike);
        verify(comment).decreaseLikeCount();
    }

    @Test
    void delete_ShouldThrow_WhenCommentDoesNotExist() {
        UUID commentId = UUID.randomUUID();
        UUID requestUserId = UUID.randomUUID();

        when(commentRepository.findById(commentId))
                .thenReturn(Optional.empty());

        assertThrows(CommentLikeException.class,
                () -> commentLikeService.delete(commentId, requestUserId));

        verify(userRepository, never()).findById(any());
        verify(commentLikeRepository, never())
                .findByCommentIdAndUserId(any(), any());

        verify(commentLikeRepository, never()).delete(any());
    }

    @Test
    void delete_ShouldThrow_WhenUserDoesNotExist() {
        UUID commentId = UUID.randomUUID();
        UUID requestUserId = UUID.randomUUID();

        Comment comment = mock(Comment.class);

        when(commentRepository.findById(commentId))
                .thenReturn(Optional.of(comment));

        when(userRepository.findById(requestUserId))
                .thenReturn(Optional.empty());

        assertThrows(CommentLikeException.class,
                () -> commentLikeService.delete(commentId, requestUserId));

        verify(commentLikeRepository, never())
                .findByCommentIdAndUserId(any(), any());

        verify(commentLikeRepository, never()).delete(any());
        verify(comment, never()).decreaseLikeCount();
    }

    @Test
    void delete_ShouldThrow_WhenCommentLikeDoesNotExist() {
        UUID commentId = UUID.randomUUID();
        UUID requestUserId = UUID.randomUUID();

        Comment comment = mock(Comment.class);
        User user = mock(User.class);

        when(commentRepository.findById(commentId))
                .thenReturn(Optional.of(comment));

        when(userRepository.findById(requestUserId))
                .thenReturn(Optional.of(user));

        when(commentLikeRepository.findByCommentIdAndUserId(commentId, requestUserId))
                .thenReturn(Optional.empty());

        assertThrows(CommentLikeException.class,
                () -> commentLikeService.delete(commentId, requestUserId));

        verify(commentLikeRepository, never()).delete(any());
        verify(comment, never()).decreaseLikeCount();
    }
}