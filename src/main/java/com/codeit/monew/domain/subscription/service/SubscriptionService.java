package com.codeit.monew.domain.subscription.service;

import com.codeit.monew.domain.interest.entity.Interest;
import com.codeit.monew.domain.interest.repository.InterestRepository;
import com.codeit.monew.domain.subscription.dto.response.SubscriptionDto;
import com.codeit.monew.domain.subscription.entity.Subscription;
import com.codeit.monew.domain.subscription.repository.SubscriptionRepository;
import com.codeit.monew.domain.user.entity.User;
import com.codeit.monew.domain.user.repository.UserRepository;
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
        // TODO: 관심사 / 사용자 예외 생성 후 적용
        Interest foundInterest = interestRepository.findById(interestId)
            .orElseThrow(() -> new RuntimeException("구독 정보 없음"));
        User foundUser = userRepository.findById(requestUserId)
            .orElseThrow(() -> new RuntimeException("사용자 정보 없음"));

        Subscription newSubscription = Subscription.create(foundInterest, foundUser);
        Subscription savedSubscription = subscriptionRepository.save(newSubscription);

        // 구독자 수 증가
        // TODO: 동시성 문제 발생할 수도 있음 (현재는 고려 X)
        foundInterest.increaseSubscriberCount();

        log.info("관심사 구독 완료. SubscriptionId: {}", savedSubscription.getId());
        return SubscriptionDto.from(savedSubscription);
    }
}
