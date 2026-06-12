package com.codeit.monew.domain.interest.dto.response;

import com.codeit.monew.domain.interest.entity.Interest;
import java.util.List;
import java.util.UUID;

public record InterestDto(
    UUID id,
    String name,
    List<String> keywords,
    Long subscriberCount,
    boolean subscribedByMe
) {

  // TODO: 요청자 ID가 들어오지 않을 시 -> subscribedByMe = false로 지정
  public static InterestDto from(Interest interest) {
    return new InterestDto(
        interest.getId(),
        interest.getName(),
        interest.getKeywords(),
        interest.getSubscriberCount(),
        false);
  }

  public static InterestDto of(Interest interest, boolean subscribedByMe) {
    return new InterestDto(
        interest.getId(),
        interest.getName(),
        interest.getKeywords(),
        interest.getSubscriberCount(),
        subscribedByMe);
  }
}
