package com.codeit.monew.domain.userActivity.dto;

import java.util.List;
import java.util.UUID;

public record UserActivityDto(
        UUID userId,
        List<UserActivitySubscriptionDto> subscriptions,
        List<UserActivityCommentDto> comments,
        List<UserActivityCommentLikeDto> commentLikes,
        List<UserActivityArticleViewDto> articleViews
) {
}
