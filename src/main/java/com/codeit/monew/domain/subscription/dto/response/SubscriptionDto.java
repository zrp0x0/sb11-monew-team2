package com.codeit.monew.domain.subscription.dto.response;

import com.codeit.monew.domain.subscription.entity.Subscription;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record SubscriptionDto(
    UUID id,
    UUID interestId,
    String interestName,
    List<String> interestKeywords,
    Long interestSubscriberCount,
    LocalDateTime createdAt
) {

    public static SubscriptionDto from(Subscription subscription) {
        return new SubscriptionDto(
            subscription.getId(),
            subscription.getInterest().getId(),
            subscription.getInterest().getName(),
            subscription.getInterest().getKeywords(),
            subscription.getInterest().getSubscriberCount(),
            subscription.getCreatedAt()
        );
    }
}
