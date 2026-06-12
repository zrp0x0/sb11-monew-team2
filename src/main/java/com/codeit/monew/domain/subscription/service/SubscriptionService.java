package com.codeit.monew.domain.subscription.service;

import com.codeit.monew.domain.interest.entity.Interest;
import com.codeit.monew.domain.interest.exception.InterestErrorCode;
import com.codeit.monew.domain.interest.exception.InterestException;
import com.codeit.monew.domain.interest.repository.InterestRepository;
import com.codeit.monew.domain.subscription.dto.response.SubscriptionDto;
import com.codeit.monew.domain.subscription.entity.Subscription;
import com.codeit.monew.domain.subscription.repository.SubscriptionRepository;
import com.codeit.monew.domain.user.entity.User;
import com.codeit.monew.domain.user.exception.UserErrorCode;
import com.codeit.monew.domain.user.exception.UserException;
import com.codeit.monew.domain.user.repository.UserRepository;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionService {

  private final SubscriptionRepository subscriptionRepository;
  private final InterestRepository interestRepository;
  private final UserRepository userRepository;

  /**
   * 관심사 구독
   */
  @Transactional
  public SubscriptionDto createSubscription(UUID interestId, UUID requestUserId) {
    log.debug("관심사 구독 시작 - interestId: {}", interestId);

    Interest foundInterest = interestRepository.findById(interestId)
        .orElseThrow(() -> new InterestException(InterestErrorCode.INTEREST_NOT_FOUND,
            Map.of("interestId", interestId)));

    // 이미 구독 중인지 확인 (이미 구독 중이라면 원래 구독 정보를 그냥 반환)
    Optional<Subscription> foundSubscription =
        subscriptionRepository.findByInterestIdAndUserIdWithInterest(interestId, requestUserId);

    if (foundSubscription.isPresent()) {
      return SubscriptionDto.from(foundSubscription.get());
    }

    User foundUser = userRepository.findById(requestUserId)
        .orElseThrow(
            () -> new UserException(UserErrorCode.USER_NOT_FOUND, Map.of("userId", requestUserId)));

    Subscription newSubscription = Subscription.create(foundInterest, foundUser);
    subscriptionRepository.save(newSubscription);

    // 구독자 수 증가
    // 동시성 문제 해결 (DB 원자적 업데이트 쿼리 실행)
    interestRepository.incrementSubscriberCount(interestId);

    // DTO에 구독자 수를 보내줘야 하므로 DB에서 구독 정보를 꺼냄
    Subscription updatedSubscription = subscriptionRepository.findByInterestIdAndUserIdWithInterest(interestId, requestUserId)
            .orElseThrow(() -> new InterestException(InterestErrorCode.SUBSCRIPTION_NOT_FOUND, Map.of("interestId", interestId, "userId", requestUserId)));

    log.info("관심사 구독 완료 - interestId: {}, subscriptionId: {}", interestId, updatedSubscription.getId());
    return SubscriptionDto.from(updatedSubscription);
  }

  /**
   * 관심사 구독 취소
   */
  @Transactional
  public void cancelSubscription(UUID interestId, UUID requestUserId) {
    log.debug("관심사 구독 취소 시작 - interestId: {}", interestId);

    if(!interestRepository.existsById(interestId)) {
      throw new InterestException(InterestErrorCode.INTEREST_NOT_FOUND, Map.of("interestId", interestId));
    }

    // 구독 취소 (삭제 쿼리를 날리고 삭제된 행 개수를 반환받음)
    int deletedCount = subscriptionRepository.deleteByInterestIdAndUserId(interestId, requestUserId);

    // 구독 중이지 않으면 그냥 종료 (멱등성 보장)
    if (deletedCount == 0) {
      log.info("관심사 구독 취소(이미 취소된 상태) - interestId: {}", interestId);
      return;
    }

    // 구독자 수 감소
    // 동시성 문제 해결 (DB 원자적 업데이트 쿼리 실행)
    interestRepository.decrementSubscriberCount(interestId);

    log.info("관심사 구독 취소 완료 - interestId: {}", interestId);
  }
}
