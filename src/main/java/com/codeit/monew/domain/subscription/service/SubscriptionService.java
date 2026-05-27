package com.codeit.monew.domain.subscription.service;

import com.codeit.monew.domain.interest.entity.Interest;
import com.codeit.monew.domain.interest.exception.InterestErrorCode;
import com.codeit.monew.domain.interest.exception.InterestException;
import com.codeit.monew.domain.interest.repository.InterestRepository;
import com.codeit.monew.domain.subscription.dto.response.SubscriptionDto;
import com.codeit.monew.domain.subscription.entity.Subscription;
import com.codeit.monew.domain.subscription.repository.SubscriptionRepository;
import com.codeit.monew.domain.user.entity.User;
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

    // TODO: Interest / User Repository 머지되면 그걸로 변경
    private final SubscriptionRepository subscriptionRepository;
    private final InterestRepository interestRepository;
    private final UserRepository userRepository;

    /**
     * 관심사 구독
     */
    @Transactional
    public SubscriptionDto createSubscription(UUID interestId, UUID requestUserId) {
        // 이미 구독 중인지 확인 (이미 구독 중이라면 원래 구독 정보를 그냥 반환)
        Optional<Subscription> foundSubscription =
            subscriptionRepository.findByInterestIdAndUserIdWithInterest(interestId, requestUserId);

        if (foundSubscription.isPresent()) {
            return SubscriptionDto.from(foundSubscription.get());
        }

        // 새로운 구독 생성
        // TODO: 사용자 예외 생성 후 적용
        Interest foundInterest = interestRepository.findById(interestId)
            .orElseThrow(() -> new InterestException(InterestErrorCode.INTEREST_NOT_FOUND,
                Map.of("interestId", interestId)));
        User foundUser = userRepository.findById(requestUserId)
            .orElseThrow(() -> new RuntimeException("사용자 정보 없음"));

        Subscription newSubscription = Subscription.create(foundInterest, foundUser);
        Subscription savedSubscription = subscriptionRepository.save(newSubscription);

        // 구독자 수 증가
        // TODO: 동시성 문제 발생할 수도 있음 (현재는 고려 X)
        foundInterest.increaseSubscriberCount(); // 더티 체크

        log.info("관심사 구독 완료. SubscriptionId: {}", savedSubscription.getId());
        return SubscriptionDto.from(savedSubscription);
    }

    /**
     * 관심사 구독 취소
     */
    @Transactional
    public void cancelSubscription(UUID interestId, UUID requestUserId) {

        // 관심사 정보가 유효한지 확인 - 있으면 영속성 컨텍스트로 가지고 옴
        Interest foundInterest = interestRepository.findById(interestId)
            .orElseThrow(() -> new InterestException(
                InterestErrorCode.INTEREST_NOT_FOUND, Map.of("interestId", interestId)));

        // 구독 중인지 확인
        Optional<Subscription> foundSubscription =
            subscriptionRepository.findByInterestIdAndUserId(interestId, requestUserId);

        // TODO: 구독 중이지 않으면 그냥 종료 (취소 성공 응답은 보내는 문제 발생)
        // - 구독하지 않은 경우 구독 취소 요청을 보내서 아무 행동 없이 return 해도 구독 취소 성공(OK)를 내보는내야할까?
        if (foundSubscription.isEmpty()) {
            return;
        }

        // 구독 취소
        subscriptionRepository.delete(foundSubscription.get());

        // 구독자 수 감소
        // TODO: 동시성 문제 발생할 수도 있음 (현재는 고려 X)
        foundInterest.decreaseSubscriberCount(); // 더티 체크

        log.info("관심사 구독 취소 완료. SubscriptionId: {}", foundSubscription.get().getId());
    }
}
