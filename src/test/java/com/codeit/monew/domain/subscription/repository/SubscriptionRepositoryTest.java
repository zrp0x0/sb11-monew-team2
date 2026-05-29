package com.codeit.monew.domain.subscription.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.codeit.monew.domain.interest.entity.Interest;
import com.codeit.monew.domain.subscription.entity.Subscription;
import com.codeit.monew.domain.user.entity.User;
import com.codeit.monew.global.config.JpaAuditingConfig;
import com.codeit.monew.global.config.QuerydslConfig;
import java.util.List;
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