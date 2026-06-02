package com.codeit.monew.domain.userActivity.dto;

import com.codeit.monew.domain.subscription.entity.Subscription;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record UserActivitySubscriptionDto(
        UUID id,
        UUID interestId,
        String interestName,
        List<String> interestKeywords,
        Long interestSubscriberCount,
        LocalDateTime createdAt
) {

    public static UserActivitySubscriptionDto from(Subscription subscription) {
        return new UserActivitySubscriptionDto(
                subscription.getId(),
                subscription.getInterest().getId(),
                subscription.getInterest().getName(),
                subscription.getInterest().getKeywords(),
                subscription.getInterest().getSubscriberCount(),
                subscription.getCreatedAt()
        );
    }
}
