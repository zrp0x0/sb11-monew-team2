package com.codeit.monew.domain.userActivity.dto;

import com.codeit.monew.domain.comment.entity.Comment;
import java.time.LocalDateTime;
import java.util.UUID;

public record UserActivityCommentDto(
        UUID id,
        UUID articleId,
        String articleTitle,
        UUID userId,
        String userNickname,
        String content,
        int likeCount,
        LocalDateTime createdAt
) {

    public static UserActivityCommentDto from(Comment comment) {
        return new UserActivityCommentDto(
                comment.getId(),
                comment.getArticle().getId(),
                comment.getArticle().getTitle(),
                comment.getUser().getId(),
                comment.getUser().getNickname(),
                comment.getContent(),
                comment.getLikeCounts(),
                comment.getCreatedAt()
        );
    }
}
