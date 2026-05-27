package com.codeit.monew.domain.subscription.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.codeit.monew.domain.interest.entity.Interest;
import com.codeit.monew.domain.interest.exception.InterestException;
import com.codeit.monew.domain.interest.repository.InterestRepository;
import com.codeit.monew.domain.subscription.dto.response.SubscriptionDto;
import com.codeit.monew.domain.subscription.repository.SubscriptionRepository;
import com.codeit.monew.domain.user.entity.User;
import com.codeit.monew.domain.user.repository.UserRepository;
import com.codeit.monew.domain.user.service.UserService;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Transactional
@SpringBootTest
class SubscriptionServiceTest {

    @Autowired
    EntityManager em;
    @Autowired
    InterestRepository interestRepository;
    @Autowired
    UserRepository userRepository;
    @Autowired
    SubscriptionRepository subscriptionRepository;
    @Autowired
    SubscriptionService subscriptionService;
    @Autowired
    UserService userService;

    /**
     * 관심사 구독
     */
    @Test
    @DisplayName("구독을 하지 않았을 때, 새로운 구독을 만들어서 반환한다.")
    void createSubscriptionTest_Success() {
        // given
        Interest newInterest = Interest.create("Test1", List.of("Keyword1", "Keyword2"));
        interestRepository.save(newInterest);

        User newUser = User.create("test1@email.com", "test1", "password123");
        userRepository.save(newUser);

        // when
        em.flush();
        em.clear();
        SubscriptionDto response = subscriptionService.createSubscription(newInterest.getId(),
            newUser.getId());

        // then
        assertThat(response.interestId()).isEqualTo(newInterest.getId());
        assertThat(response.interestName()).isEqualTo(newInterest.getName());
        assertThat(response.interestSubscriberCount()).isEqualTo(1L);
    }

    @Test
    @DisplayName("이미 구독을 했을 때, 원래 구독 정보를 반환한다.")
    void createSubscriptionTest_Success_Already_Subscription() {
        // given
        Interest newInterest = Interest.create("Test1", List.of("Keyword1", "Keyword2"));
        interestRepository.save(newInterest);

        User newUser = User.create("test1@email.com", "test1", "password123");
        userRepository.save(newUser);

        em.flush();
        em.clear();
        SubscriptionDto response1 = subscriptionService.createSubscription(newInterest.getId(),
            newUser.getId());

        // when
        em.flush();
        em.clear();
        SubscriptionDto response2 = subscriptionService.createSubscription(newInterest.getId(),
            newUser.getId());

        // then
        assertThat(response2.id()).isEqualTo(response1.id());
    }

    @Test
    @DisplayName("관심사 정보가 없으면 실패한다.")
    void createSubscriptionTest_Fail_Not_Found_Interest() {
        // given
        UUID interestId = UUID.randomUUID();

        User newUser = User.create("test1@email.com", "test1", "password123");
        userRepository.save(newUser);

        // when & then
        // TODO: InterestException으로 변경
        em.flush();
        em.clear();
        assertThatThrownBy(
            () -> subscriptionService.createSubscription(interestId, newUser.getId()))
            .isInstanceOf(InterestException.class);
    }

    /**
     * 관심사 구독 취소
     */
    @Test
    @DisplayName("구독 중인 관심사를 취소하면 구독 정보가 삭제되고 구독자 수가 감소한다.")
    void cancelSubscriptionTest_Success() {
        // given
        Interest newInterest = Interest.create("Test1", List.of("Keyword1", "Keyword2"));
        interestRepository.save(newInterest);

        User newUser = User.create("test1@email.com", "test1", "password123");
        userRepository.save(newUser);

        // 우선 구독 처리 (구독자 수 1 증가)
        subscriptionService.createSubscription(newInterest.getId(), newUser.getId());
        em.flush();
        em.clear();

        // when
        subscriptionService.cancelSubscription(newInterest.getId(), newUser.getId());
        em.flush();
        em.clear();

        // then
        // 구독 정보가 삭제되었는지 확인
        boolean isSubscribed = subscriptionRepository.findByInterestIdAndUserIdWithInterest(
            newInterest.getId(), newUser.getId()).isPresent();
        assertThat(isSubscribed).isFalse();

        // 관심사 구독자 수가 0으로 감소했는지 확인
        Interest foundInterest = interestRepository.findById(newInterest.getId()).orElseThrow();
        assertThat(foundInterest.getSubscriberCount()).isEqualTo(0L);
    }

    @Test
    @DisplayName("구독하지 않은 상태에서 구독 취소를 요청하면 아무런 예외 없이 정상 종료(멱등성)되며, 구독자 수는 변함이 없다.")
    void cancelSubscriptionTest_Success_Not_Subscribed() {
        // given
        Interest newInterest = Interest.create("Test1", List.of("Keyword1", "Keyword2"));
        interestRepository.save(newInterest);

        User newUser = User.create("test1@email.com", "test1", "password123");
        userRepository.save(newUser);

        em.flush();
        em.clear();

        // when (예외가 발생하지 않는지 검증)
        subscriptionService.cancelSubscription(newInterest.getId(), newUser.getId());
        em.flush();
        em.clear();

        // then
        // 원래 상태(구독자 수 0)가 그대로 유지되는지 확인
        Interest foundInterest = interestRepository.findById(newInterest.getId()).orElseThrow();
        assertThat(foundInterest.getSubscriberCount()).isEqualTo(0L);
    }
}