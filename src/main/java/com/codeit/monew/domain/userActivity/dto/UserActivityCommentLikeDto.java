package com.codeit.monew.domain.userActivity.dto;

import com.codeit.monew.domain.comment.entity.Comment;
import com.codeit.monew.domain.commentLike.entity.CommentLike;
import java.time.LocalDateTime;
import java.util.UUID;

public record UserActivityCommentLikeDto(
        UUID id,
        UUID commentId,
        UUID articleId,
        String articleTitle,
        UUID commentUserId,
        String commentUserNickname,
        String commentContent,
        int commentLikeCount,
        LocalDateTime commentCreatedAt,
        LocalDateTime createdAt
) {

    public static UserActivityCommentLikeDto from(CommentLike commentLike) {
        Comment comment = commentLike.getComment();
        return new UserActivityCommentLikeDto(
                commentLike.getId(),
                comment.getId(),
                comment.getArticle().getId(),
                comment.getArticle().getTitle(),
                comment.getUser().getId(),
                comment.getUser().getNickname(),
                comment.getContent(),
                comment.getLikeCounts(),
                comment.getCreatedAt(),
                commentLike.getCreatedAt()
        );
    }
}
