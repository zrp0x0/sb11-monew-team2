package com.codeit.monew.domain.subscription.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.codeit.monew.domain.interest.entity.Interest;
import com.codeit.monew.domain.subscription.entity.Subscription;
import com.codeit.monew.domain.user.entity.User;
import com.codeit.monew.global.config.JpaAuditingConfig;
import com.codeit.monew.global.config.QuerydslConfig;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@Import({JpaAuditingConfig.class, QuerydslConfig.class})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class SubscriptionRepositoryTest {

  @Autowired
  private SubscriptionRepository subscriptionRepository;

  @Autowired
  private TestEntityManager tem;

  @Test
  @DisplayName("구독 관심사 ID 조회 - 특정 유저가 구독중인 관심사 ID 목록을 반환한다.")
  void findSubscribedInterestIds_success() {
    //given
    User user = User.create("test@test", "유저", "pass");
    tem.persist(user);

    Interest interestA = Interest.create("구독한 관심사A", List.of("키워드"));
    Interest interestB = Interest.create("구독한 관심사B", List.of("키워드"));
    Interest interestC = Interest.create("미구독 관심사", List.of("키워드"));
    tem.persist(interestA);
    tem.persist(interestB);
    tem.persist(interestC);

    Subscription subscriptionA = Subscription.create(interestA, user);
    Subscription subscriptionB = Subscription.create(interestB, user);
    tem.persist(subscriptionA);
    tem.persist(subscriptionB);

    tem.flush();
    tem.clear();

    List<UUID> interestIds = List.of(interestA.getId(), interestB.getId());

    //when
    List<UUID> result = subscriptionRepository.findSubscribedInterestIds(user.getId(), interestIds);

    //then
    assertThat(result).hasSize(2)
        .containsExactlyInAnyOrder(interestA.getId(), interestB.getId())
        .doesNotContain(interestC.getId());
  }

  @Test
  @DisplayName("구독 관심사 ID 조회 - 유저가 구독한 관심사가 없을 경우 빈 리스트를 반환한다.")
  void findSubscribedInterestIds_empty() {
    //given
    User user = User.create("test@test", "유저", "pass");
    tem.persist(user);

    Interest interestA = Interest.create("구독한 관심사A", List.of("키워드"));
    tem.persist(interestA);

    tem.flush();
    tem.clear();

    List<UUID> interestIds = List.of(interestA.getId());

    //when
    List<UUID> result = subscriptionRepository.findSubscribedInterestIds(user.getId(), interestIds);

    //then
    assertThat(result).isEmpty();
    assertThat(result).isNotNull();
  }

  @Test
  @DisplayName("관심사 ID로 연관된 모든 구독 내역을 벌크 삭제한다.")
  void deleteByInterestId_success() {
    //given
    User user = User.create("test@test", "유저", "pass");
    tem.persist(user);

    Interest interestA = Interest.create("관심사A", List.of("키워드"));
    Interest interestB = Interest.create("관심사B", List.of("키워드"));
    tem.persist(interestA);
    tem.persist(interestB);

    Subscription subscriptionA = Subscription.create(interestA, user);
    Subscription subscriptionB = Subscription.create(interestB, user);
    tem.persist(subscriptionA);
    tem.persist(subscriptionB);

    tem.flush();
    tem.clear();

    //when
    subscriptionRepository.deleteByInterestId(interestA.getId());

    //then
    List<Subscription> subscriptionList = subscriptionRepository.findAll();

    assertThat(subscriptionList)
        .extracting(Subscription::getId)
        .containsExactly(subscriptionB.getId())
        .doesNotContain(subscriptionA.getId());
  }
}