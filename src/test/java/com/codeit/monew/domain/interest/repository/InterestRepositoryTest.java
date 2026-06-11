package com.codeit.monew.domain.interest.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.codeit.monew.domain.interest.entity.Interest;
import com.codeit.monew.global.config.JpaAuditingConfig;
import com.codeit.monew.global.config.QuerydslConfig;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@Import({JpaAuditingConfig.class, QuerydslConfig.class})
@AutoConfigureTestDatabase(replace = Replace.NONE)
public class InterestRepositoryTest {

  @Autowired
  private InterestRepository interestRepository;

  @Autowired
  private TestEntityManager tem;

  @Test
  @DisplayName("유사 길이 조회 - 기준 길이(min~max) 범위 내의 관심사 목록을 반환한다.")
  void findSimilarLengthInterests_returnMatchedInterests() {
    //given
    Interest interestA = Interest.create("가", List.of("키워드"));
    Interest interestB = Interest.create("가나", List.of("키워드"));
    Interest interestC = Interest.create("가나다라", List.of("키워드"));
    Interest interestD = Interest.create("가나다라마", List.of("키워드"));

    tem.persist(interestA);
    tem.persist(interestB);
    tem.persist(interestC);
    tem.persist(interestD);

    tem.flush();
    tem.clear();

    //when
    List<Interest> result = interestRepository.findSimilarLengthInterests(2, 4); // 3글자 관심사가 들어올 경우

    //then
    assertThat(result).hasSize(2)
        .extracting(Interest::getName)
        .containsExactlyInAnyOrder("가나", "가나다라")
        .doesNotContain("가", "가나다라마");
  }

  @Test
  @DisplayName("유사 길이 조회 - 조건에 맞는 길이의 관심사가 없으면 빈 리스트를 반환한다.")
  void findSimilarLengthInterests_returnsEmptyList() {
    //given
    Interest interestA = Interest.create("가나다라", List.of("키워드"));

    tem.persistAndFlush(interestA);
    tem.clear();

    //when
    List<Interest> result = interestRepository.findSimilarLengthInterests(1, 3);

    //then
    assertThat(result).isEmpty();
    assertThat(result).isNotNull();
  }

  @Test
  @DisplayName("incrementSubscriberCount 쿼리를 실행하면 구독자 수가 1 증가하고 업데이트된 행 개수를 반환한다.")
  void incrementSubscriberCount_Success() {
    // given
    Interest interest = Interest.create("테스트 관심사", List.of("테스트"));
    interestRepository.save(interest);
    tem.flush(); tem.clear();

    // when
    int updatedCount = interestRepository.incrementSubscriberCount(interest.getId());

    // then
    assertThat(updatedCount).isEqualTo(1); // 1개의 행이 업데이트됨
    Interest foundInterest = interestRepository.findById(interest.getId()).orElseThrow();
    assertThat(foundInterest.getSubscriberCount()).isEqualTo(1L);
  }

  @Test
  @DisplayName("decrementSubscriberCount 쿼리를 실행하면 구독자 수가 1 감소한다.")
  void decrementSubscriberCount_Success() {
    // given
    Interest interest = Interest.create("테스트 관심사", List.of("테스트"));
    interestRepository.save(interest);

    // 미리 1을 증가
    interestRepository.incrementSubscriberCount(interest.getId());
    tem.flush(); tem.clear();

    // when
    int updatedCount = interestRepository.decrementSubscriberCount(interest.getId());

    // then
    assertThat(updatedCount).isEqualTo(1);
    Interest foundInterest = interestRepository.findById(interest.getId()).orElseThrow();
    assertThat(foundInterest.getSubscriberCount()).isEqualTo(0L);
  }

  @Test
  @DisplayName("구독자 수가 0일 때 decrementSubscriberCount 쿼리를 실행하면 음수로 떨어지지 않는다.")
  void decrementSubscriberCount_Prevent_Negative() {
    // given
    Interest interest = Interest.create("테스트 관심사", List.of("테스트"));
    interestRepository.save(interest);
    tem.flush(); tem.clear();

    // when
    int updatedCount = interestRepository.decrementSubscriberCount(interest.getId());

    // then
    assertThat(updatedCount).isEqualTo(0);
    Interest foundInterest = interestRepository.findById(interest.getId()).orElseThrow();
    assertThat(foundInterest.getSubscriberCount()).isEqualTo(0L);
  }
}