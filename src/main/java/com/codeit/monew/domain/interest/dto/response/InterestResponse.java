package com.codeit.monew.domain.interest.dto.response;

import com.codeit.monew.domain.interest.entity.Interest;
import java.util.List;
import java.util.UUID;

public record InterestResponse(
    UUID id,
    String name,
    List<String> keywords,
    Long subscriberCount,
    boolean subscribedByMe
) {

  public static InterestResponse from(Interest interest, boolean subscribedByMe) {
    return new InterestResponse(
        interest.getId(),
        interest.getName(),
        interest.getKeywords(),
        interest.getSubscriberCount(),
        subscribedByMe);
  }
}
