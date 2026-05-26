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

  // TODO: 요청자 ID가 들어오지 않을 시 -> subscribedByMe = false로 지정
  public static InterestResponse from(Interest interest) {
    return new InterestResponse(
        interest.getId(),
        interest.getName(),
        interest.getKeywords(),
        interest.getSubscriberCount(),
        false);
  }

  public static InterestResponse of(Interest interest, boolean subscribedByMe) {
    return new InterestResponse(
        interest.getId(),
        interest.getName(),
        interest.getKeywords(),
        interest.getSubscriberCount(),
        subscribedByMe);
  }
}
