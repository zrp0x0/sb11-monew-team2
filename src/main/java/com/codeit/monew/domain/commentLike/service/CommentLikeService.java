package com.codeit.monew.domain.commentLike.service;

import com.codeit.monew.domain.article.entity.Article;
import com.codeit.monew.domain.comment.entity.Comment;
import com.codeit.monew.domain.comment.repository.CommentRepository;
import com.codeit.monew.domain.commentLike.dto.CommentLikeDto;
import com.codeit.monew.domain.commentLike.entity.CommentLike;
import com.codeit.monew.domain.commentLike.exception.CommentLikeErrorCode;
import com.codeit.monew.domain.commentLike.exception.CommentLikeException;
import com.codeit.monew.domain.commentLike.repository.CommentLikeRepository;
import com.codeit.monew.domain.notification.listener.NotificationCreateEvent;
import com.codeit.monew.domain.user.entity.User;
import com.codeit.monew.domain.user.repository.UserRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class CommentLikeService {

    private final CommentLikeRepository commentLikeRepository;
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    public CommentLikeDto create(UUID commentId, UUID requestUserId)  {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new CommentLikeException(CommentLikeErrorCode.COMMENT_NOT_FOUND));

        User user = userRepository.findById(requestUserId)
                .orElseThrow(() -> new CommentLikeException(CommentLikeErrorCode.USER_NOT_FOUND));

        if(commentLikeRepository.findByCommentIdAndUserId(commentId, requestUserId).isPresent()) {
            throw new CommentLikeException(CommentLikeErrorCode.COMMENT_LIKE_ALREADY_EXISTS);
        }

        CommentLike commentLike = new CommentLike(comment, user);
        commentLikeRepository.save(commentLike);
        comment.increaseLikeCount();
        eventPublisher.publishEvent(NotificationCreateEvent.createByCommentLike(comment, user));
        return mapper(commentLike, comment, user);
    }

    public void delete(UUID commentId, UUID requestUserId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new CommentLikeException(CommentLikeErrorCode.COMMENT_NOT_FOUND));

        userRepository.findById(requestUserId)
                .orElseThrow(() -> new CommentLikeException(CommentLikeErrorCode.USER_NOT_FOUND));

        CommentLike commentLike = commentLikeRepository.findByCommentIdAndUserId(commentId, requestUserId)
                .orElseThrow(() -> new CommentLikeException(CommentLikeErrorCode.COMMENT_LIKE_NOT_FOUND));

        commentLikeRepository.delete(commentLike);
        comment.decreaseLikeCount();
    }

    private CommentLikeDto mapper(CommentLike commentLike, Comment comment, User likedUser) {
        Article article = comment.getArticle();
        User commentUser = comment.getUser();

        return new CommentLikeDto(
                commentLike.getId(),
                likedUser.getId(),
                commentLike.getCreatedAt(),
                comment.getId(),
                article.getId(),
                commentUser.getId(),
                commentUser.getNickname(),
                comment.getContent(),
                comment.getLikeCounts(),
                comment.getCreatedAt()
        );
    }
}
