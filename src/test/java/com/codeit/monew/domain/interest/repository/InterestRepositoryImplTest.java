package com.codeit.monew.domain.interest.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.codeit.monew.domain.interest.dto.request.InterestSearchRequest;
import com.codeit.monew.domain.interest.entity.Interest;
import com.codeit.monew.domain.interest.exception.InterestErrorCode;
import com.codeit.monew.domain.interest.exception.InterestException;
import com.codeit.monew.global.config.JpaAuditingConfig;
import com.codeit.monew.global.config.QuerydslConfig;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

@DataJpaTest
@ActiveProfiles("test")
@Import({JpaAuditingConfig.class, QuerydslConfig.class})
@AutoConfigureTestDatabase(replace = Replace.NONE)
public class InterestRepositoryImplTest {

  @Autowired
  private InterestRepositoryImpl interestRepository;

  @Autowired
  private TestEntityManager tem;

  @Test
  @DisplayName("목록 페이징 조회 - 이름 또는 키워드에 특정 단어가 포함된 관심사를 기본 정렬(이름, 내림차순)로 반환한다.")
  void findAllByCondition_filterByKeyword() {
    //given
    Interest interestA = Interest.create("축구", List.of("키워드"));
    Interest interestB = Interest.create("스포츠", List.of("축구"));
    Interest interestC = Interest.create("관심사", List.of("키워드"));

    tem.persist(interestA);
    tem.persist(interestB);
    tem.persist(interestC);
    tem.flush(); tem.clear();

    InterestSearchRequest request = new InterestSearchRequest(
        "축구", null, null, null, LocalDateTime.now(), 2
    );

    //when
    List<Interest> result = interestRepository.findAllByCondition(request);

    //then
    assertThat(result).hasSize(2)
        .extracting(Interest::getId)
        .containsExactly(interestA.getId(), interestB.getId())
        .doesNotContain(interestC.getId());
  }

  @Test
  @DisplayName("목록 페이징 실패 - 구독자 기준 정렬 시 커서 데이터 파싱 중 NumberFormatException 발생 시 INVALID_CURSOR_FORMAT 예외를 던진다.")
  void findAllByCondition_fail_invalidParsing() {
    //given
    InterestSearchRequest request = new InterestSearchRequest(
        null, "subscriberCount", null, "문자열_" + UUID.randomUUID(), LocalDateTime.now(), 2
    );

    //when & then
    assertThatThrownBy(() -> interestRepository.findAllByCondition(request))
        .isInstanceOf(InterestException.class)
        .extracting("errorCode")
        .isEqualTo(InterestErrorCode.INVALID_CURSOR_FORMAT);
  }

  @Test
  @DisplayName("목록 페이징 실패 - 구독자 기준 정렬 시 커서를 '_'로 split한 배열 길이가 2가 아닐 시 INVALID_CURSOR_FORMAT 예외를 던진다.")
  void findAllByCondition_fail_invalidLength() {
    //given
    InterestSearchRequest request = new InterestSearchRequest(
        null, "subscriberCount", null, "100", LocalDateTime.now(), 2
    );

    //when & then
    assertThatThrownBy(() -> interestRepository.findAllByCondition(request))
        .isInstanceOf(InterestException.class)
        .extracting("errorCode")
        .isEqualTo(InterestErrorCode.INVALID_CURSOR_FORMAT);
  }

  @Test
  @DisplayName("목록 페이징 정렬 - 이름(name) 기준 정렬 시 오름차순(ASC) 및 내림차순(DESC)이 정확히 적용된다.")
  void findAllByCondition_sortByName() {
    //given
    Interest interestA = Interest.create("가", List.of("키워드"));
    Interest interestB = Interest.create("나", List.of("축구"));

    tem.persist(interestB);
    tem.persist(interestA);
    tem.flush();tem.clear();

    InterestSearchRequest request = new InterestSearchRequest(
        null, "name", "ASC", null, LocalDateTime.now(), 2
    );

    //when
    List<Interest> result = interestRepository.findAllByCondition(request);

    //then
    assertThat(result).hasSize(2)
        .extracting(Interest::getName)
        .containsExactly("가", "나");
  }

  @Test
  @DisplayName("3단 타이 브레이커 1차 - 구독자 수가 다를 때 구독자 수 기준으로 정상 페이징된다.")
  void findAllByCondition_tieBreaker_1st_subscriberCount() {
    //given
    Interest interestA = Interest.create("관심사A", List.of("키워드"));
    Interest interestB = Interest.create("관심사B", List.of("키워드"));


    ReflectionTestUtils.setField(interestA, "subscriberCount", 10L);
    ReflectionTestUtils.setField(interestB, "subscriberCount", 20L);

    tem.persist(interestA);
    tem.persist(interestB);
    tem.flush(); tem.clear();

    InterestSearchRequest request = new InterestSearchRequest(
        null, "subscriberCount", "DESC", null, LocalDateTime.now(), 2
    );

    //when
    List<Interest> result = interestRepository.findAllByCondition(request);

    //then
    assertThat(result).hasSize(2)
        .extracting(Interest::getName)
        .containsExactly("관심사B", "관심사A");
  }

  @Test
  @DisplayName("3단 타이 브레이커 2차 - 이전 페이지의 cursor와 afterCursor를 전달 받아 구독자 수가 같을 때 생성 일자(최신순) 기준으로 페이징된다.")
  void findAllByCondition_tieBreaker_2nd_createdAt() {
    //given
    Interest interestA = Interest.create("관심사A", List.of("키워드"));
    Interest interestB = Interest.create("관심사B", List.of("키워드"));
    Interest interestC = Interest.create("관심사C", List.of("키워드"));

    ReflectionTestUtils.setField(interestA, "subscriberCount", 10L);
    ReflectionTestUtils.setField(interestB, "subscriberCount", 10L);
    ReflectionTestUtils.setField(interestC, "subscriberCount", 10L);

    tem.persist(interestC);
    tem.persist(interestB);
    tem.persist(interestA);
    tem.flush();

    LocalDateTime timeA = LocalDateTime.of(2026, 1, 1, 1, 1);
    LocalDateTime timeB = LocalDateTime.of(2026, 1, 1, 1, 2);
    LocalDateTime timeC = LocalDateTime.of(2026, 1, 1, 1, 3);

    tem.getEntityManager().createQuery("UPDATE Interest i SET i.createdAt = :time WHERE i.id = :id")
            .setParameter("time", timeA).setParameter("id", interestA.getId()).executeUpdate();
    tem.getEntityManager().createQuery("UPDATE Interest i SET i.createdAt = :time WHERE i.id = :id")
        .setParameter("time", timeB).setParameter("id", interestB.getId()).executeUpdate();
    tem.getEntityManager().createQuery("UPDATE Interest i SET i.createdAt = :time WHERE i.id = :id")
        .setParameter("time", timeC).setParameter("id", interestC.getId()).executeUpdate();

    tem.clear();

    InterestSearchRequest firstRequest = new InterestSearchRequest(
        null, "subscriberCount", "DESC", null, LocalDateTime.now(), 1
    );

    List<Interest> firstPage = interestRepository.findAllByCondition(firstRequest);

    assertThat(firstPage).hasSize(2)
        .extracting(Interest::getName)
        .containsExactly("관심사C", "관심사B");

    String cursor = firstPage.get(1).getSubscriberCount() + "_" + firstPage.get(1).getId();
    LocalDateTime afterCursor = firstPage.get(1).getCreatedAt();
    InterestSearchRequest secondRequest = new InterestSearchRequest(
        null, "subscriberCount", "DESC", cursor, afterCursor, 1
    );

    //when
    List<Interest> secondPage = interestRepository.findAllByCondition(secondRequest);

    //then
    assertThat(secondPage).hasSize(1)
        .extracting(Interest::getName)
        .containsExactly("관심사A");
  }

  @Test
  @DisplayName("3단 타이 브레이커 3차 - 구독자 수와 생성 일자가 모두 같을 때 관심사 ID(작은순) 기준으로 페이징된다.")
  void findAllByCondition_tieBreaker_3rd_uuid() {
    //given
    Interest interestA = Interest.create("관심사A", List.of("키워드"));
    Interest interestB = Interest.create("관심사B", List.of("키워드"));

    ReflectionTestUtils.setField(interestA, "subscriberCount", 10L);
    ReflectionTestUtils.setField(interestB, "subscriberCount", 10L);

    tem.persist(interestA);
    tem.persist(interestB);
    tem.flush();

    LocalDateTime sameTime = LocalDateTime.of(2026, 1, 1, 1, 1);
    tem.getEntityManager()
        .createQuery("UPDATE Interest i SET i.createdAt = :sameTime")
        .setParameter("sameTime", sameTime)
        .executeUpdate();

    tem.clear();

    Interest first = interestA.getId().toString().compareTo(interestB.getId().toString()) < 0 ? interestA : interestB;
    Interest second = first == interestA ? interestB : interestA;

    InterestSearchRequest request = new InterestSearchRequest(
        null, "subscriberCount", "DESC", null, LocalDateTime.now(), 1
    );

    //when
    List<Interest> result = interestRepository.findAllByCondition(request);

    //then
    assertThat(result).hasSize(2)
        .extracting(Interest::getId)
        .containsExactly(first.getId(), second.getId());
  }

  @Test
  @DisplayName("커서 페이징 동작 - 이름 정렬 시 이전 페이지의 마지막 커서를 전달하면 다음 페이지 데이터를 정확히 가져온다.")
  void findAllByCondition_cursor_name() {
    //given
    Interest interestA = Interest.create("나", List.of("키워드"));
    Interest interestB = Interest.create("다", List.of("키워드"));
    Interest interestC = Interest.create("라", List.of("키워드"));

    tem.persist(interestA);
    tem.persist(interestB);
    tem.persist(interestC);
    tem.flush(); tem.clear();

    InterestSearchRequest firstRequest = new InterestSearchRequest(
        null, "name", "ACS", "가", LocalDateTime.now(), 1
    );

    List<Interest> firstPage = interestRepository.findAllByCondition(firstRequest);

    assertThat(firstPage).hasSize(2)
        .extracting(Interest::getName)
        .containsExactly("나", "다");

    String nextCursor = firstPage.get(1).getName();

    InterestSearchRequest secondRequest = new InterestSearchRequest(
        null, "name", "ACS", nextCursor, LocalDateTime.now(), 1
    );

    //when
    List<Interest> secondPage = interestRepository.findAllByCondition(secondRequest);

    //then
    assertThat(secondPage).hasSize(1)
        .extracting(Interest::getName)
        .containsExactly("라");
  }
}