package com.codeit.monew.domain.notification.listener;

import com.codeit.monew.domain.comment.entity.Comment;
import com.codeit.monew.domain.notification.entity.NotificationResourceType;
import com.codeit.monew.domain.user.entity.User;
import java.util.UUID;

public record NotificationCreateEvent(
    UUID receiverId,
    String content,
    NotificationResourceType resourceType,
    UUID resourceId
) {

    // == 댓글 좋아요 전용 이벤트 생성기 ==
    public static NotificationCreateEvent createByCommentLike(Comment comment, User sender) {
        String content = String.format("[%s]님이 나의 댓글을 좋아합니다.", sender.getNickname());

        return new NotificationCreateEvent(
            comment.getUser().getId(),
            content,
            NotificationResourceType.COMMENT,
            comment.getId()
        );
    }
}
