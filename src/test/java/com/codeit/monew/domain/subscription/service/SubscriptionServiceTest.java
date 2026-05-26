package com.codeit.monew.domain.subscription.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.codeit.monew.domain.interest.entity.Interest;
import com.codeit.monew.domain.interest.repository.InterestRepository;
import com.codeit.monew.domain.subscription.dto.response.SubscriptionDto;
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
    SubscriptionService subscriptionService;
    @Autowired
    private UserService userService;

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
            .isInstanceOf(RuntimeException.class);
    }

}