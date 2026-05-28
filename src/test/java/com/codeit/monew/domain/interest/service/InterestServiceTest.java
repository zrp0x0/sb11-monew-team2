package com.codeit.monew.domain.interest.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

import com.codeit.monew.domain.interest.dto.request.InterestRegisterRequest;
import com.codeit.monew.domain.interest.dto.request.InterestSearchRequest;
import com.codeit.monew.domain.interest.dto.request.InterestUpdateRequest;
import com.codeit.monew.domain.interest.dto.response.InterestResponse;
import com.codeit.monew.domain.interest.entity.Interest;
import com.codeit.monew.domain.interest.exception.InterestErrorCode;
import com.codeit.monew.domain.interest.exception.InterestException;
import com.codeit.monew.domain.interest.repository.InterestRepository;
import com.codeit.monew.domain.subscription.repository.SubscriptionRepository;
import com.codeit.monew.domain.user.exception.UserErrorCode;
import com.codeit.monew.domain.user.exception.UserException;
import com.codeit.monew.domain.user.repository.UserRepository;
import com.codeit.monew.global.dto.CursorPageResponse;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
public class InterestServiceTest {

  @Mock
  private InterestRepository interestRepository;

  @Mock
  private UserRepository userRepository;

  @Mock
  private SubscriptionRepository subscriptionRepository;

  @InjectMocks
  private InterestService interestService;

  @Test
  @DisplayName("관심사 등록 성공")
  void createInterest_success() {
    //given
    InterestRegisterRequest request = new InterestRegisterRequest("스포츠", List.of("축구"));

    given(interestRepository.existsByName(request.name()))
        .willReturn(false);

    Interest interest = createMockInterest(UUID.randomUUID(), "스포츠", 10L);

    given(interestRepository.save(any(Interest.class)))
        .willReturn(interest);

    //when
    InterestResponse result = interestService.createInterest(request);

    //then
    assertThat(result).isNotNull();
    assertThat(result.name()).isEqualTo("스포츠");
    assertThat(result.keywords()).hasSize(2);
  }

  @Test
  @DisplayName("관심사 등록 실패 - 중복된 관심사가 있을 시 INTEREST_ALREADY_EXISTS 예외 발생")
  void createInterest_fail_already_exists() {
    //given
    InterestRegisterRequest request = new InterestRegisterRequest("스포츠", List.of("체육"));

    given(interestRepository.existsByName(request.name()))
        .willReturn(true);

    //when & then
    assertThatThrownBy(() -> interestService.createInterest(request))
        .isInstanceOf(InterestException.class)
        .extracting("errorCode").isEqualTo(InterestErrorCode.INTEREST_ALREADY_EXISTS);
  }

  @ParameterizedTest(name = "[{index}] 요청: {0}, 기존: {1}")
  @MethodSource("similaritySuccessCases")
  @DisplayName("관심사 등록 성공 - 유사도가 기준치 미만이거나 길이가 확연히 다를 시 등록 성공")
  void createInterest_success_not_similar(String newName, String existingName) {
    //given
    InterestRegisterRequest request = new InterestRegisterRequest(newName, List.of("테스트"));
    Interest existingInterest = createMockInterest(UUID.randomUUID(), existingName, 10L);

    given(interestRepository.existsByName(newName))
        .willReturn(false);
    given(interestRepository.findSimilarLengthInterests(anyInt(), anyInt()))
        .willReturn(List.of(existingInterest));

    Interest interest = createMockInterest(UUID.randomUUID(), newName, 10L);

    given(interestRepository.save(any(Interest.class)))
        .willReturn(interest);

    //when & then
    assertDoesNotThrow(() -> interestService.createInterest(request));
  }

  private static Stream<Arguments> similaritySuccessCases() {
    return Stream.of(
        Arguments.of("축구", "농구"),
        Arguments.of("데이터분석", "데이터과학")
    );
  }

  @ParameterizedTest(name = "[{index}] 요청: {0}, 기존: {1}")
  @MethodSource("similarityFailCases")
  @DisplayName("관심사 등록 실패 - 관심사 유사도 기준 초과 시 SIMILAR_INTEREST_EXISTS 예외 발생")
  void createInterest_fail_similar_interest_exists(String newName, String existingName) {
    //given
    InterestRegisterRequest request = new InterestRegisterRequest(newName, List.of("테스트"));
    Interest existingInterest = createMockInterest(UUID.randomUUID(), existingName, 10L);

    given(interestRepository.existsByName(request.name()))
        .willReturn(false);
    given(interestRepository.findSimilarLengthInterests(anyInt(), anyInt()))
        .willReturn(List.of(existingInterest));

    //when & then
    assertThatThrownBy(() -> interestService.createInterest(request))
        .isInstanceOf(InterestException.class)
        .extracting("errorCode").isEqualTo(InterestErrorCode.SIMILAR_INTEREST_EXISTS);
  }

  private static Stream<Arguments> similarityFailCases() {
    return Stream.of(
        Arguments.of("축구", "츅구"),
        Arguments.of("스포츠", "스포쓰"),
        Arguments.of("프론트엔드", "프론트엔두")
    );
  }

  @Test
  @DisplayName("관심사 수정 성공")
  void updateInterest_success() {
    //given
    UUID interestId = UUID.randomUUID();
    List<String> oldKeywords = List.of("수영");
    List<String> newKeywords = List.of("축구", "야구");

    InterestUpdateRequest request = new InterestUpdateRequest(newKeywords);
    Interest interest = Interest.create("스포츠", oldKeywords);

    ReflectionTestUtils.setField(interest, "id", interestId);

    given(interestRepository.findById(interestId))
        .willReturn(Optional.of(interest));

    //when
    InterestResponse result = interestService.updateInterest(interestId, request);

    //then
    assertThat(result.id()).isEqualTo(interestId);
    assertThat(result.keywords()).doesNotContain("수영");
    assertThat(result.keywords()).hasSize(2);
  }

  @Test
  @DisplayName("관심사 키워드 수정 실패 - 존재하지 않는 ID를 넘길 시 INTEREST_NOT_FOUND 예외 발생")
  void updateKeywords_fail_not_found() {
    //given
    UUID invalidId = UUID.randomUUID();
    InterestUpdateRequest request = new InterestUpdateRequest(List.of("새키워드"));

    given(interestRepository.findById(invalidId))
        .willReturn(Optional.empty());

    //when & then
    assertThatThrownBy(() -> interestService.updateInterest(invalidId, request))
        .isInstanceOf(InterestException.class)
        .extracting("errorCode").isEqualTo(InterestErrorCode.INTEREST_NOT_FOUND);
  }

  @Test
  @DisplayName("관심사 목록 조회 성공 - 다음 페이지가 있는 경우(hasNext = true")
  void searchInterest_success_has_next_page() {
    //given
    UUID userId = UUID.randomUUID();
    InterestSearchRequest request = new InterestSearchRequest(null, "name", "ASC", null, null, 2);

    given(userRepository.existsById(userId))
        .willReturn(true);

    Interest interestA = createMockInterest(UUID.randomUUID(), "골프", 10L);
    Interest interestB = createMockInterest(UUID.randomUUID(), "농구", 20L);
    Interest interestC = createMockInterest(UUID.randomUUID(), "축구", 30L);

    List<Interest> interestList = List.of(interestA, interestB, interestC);
    given(interestRepository.findAllByCondition(request))
        .willReturn(interestList);

    List<UUID> subscribedInterestIds = List.of(interestA.getId(), interestC.getId());
    given(subscriptionRepository.findSubscribedInterestIds(eq(userId), any()))
        .willReturn(subscribedInterestIds);

    //when
    CursorPageResponse<InterestResponse> result = interestService.searchInterest(userId, request);

    //then
    assertThat(result.content()).hasSize(2);
    assertThat(result.nextCursor()).isEqualTo("농구");
    assertThat(result.nextAfter()).isEqualTo(String.valueOf(interestB.getCreatedAt()));
    // 날짜 형식(YYYY-MM-DD T HH:mm:ss...) 정규식에 맞는지 검증
    assertThat(result.nextAfter()).matches("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}.*");
    assertThat(result.hasNext()).isTrue();

    assertThat(result.content().get(0).name()).isEqualTo("골프");
    assertThat(result.content().get(0).subscribedByMe()).isTrue();
    assertThat(result.content().get(1).name()).isEqualTo("농구");
    assertThat(result.content().get(1).subscribedByMe()).isFalse();
  }

  @Test
  @DisplayName("관심사 목록 조회 실패 - 존재하지 않는 유저")
  void searchInterest_fail_user_not_found() {
    //given
    UUID userId = UUID.randomUUID();
    InterestSearchRequest request = new InterestSearchRequest(null, "name", "ASC", null, null, 2);

    given(userRepository.existsById(userId))
        .willReturn(false);

    //when & then
    assertThatThrownBy(() -> interestService.searchInterest(userId, request))
        .isInstanceOf(UserException.class)
        .extracting("errorCode")
        .isEqualTo(UserErrorCode.INVALID_CREDENTIALS);
  }

  // 엔티티 생성 헬퍼 메서드
  private Interest createMockInterest(UUID id, String name, long subscriberCount) {
    Interest interest = Interest.create(name, List.of("키워드1", "키워드2"));

    ReflectionTestUtils.setField(interest, "id", id);
    ReflectionTestUtils.setField(interest, "subscriberCount", subscriberCount);
    ReflectionTestUtils.setField(interest, "createdAt", LocalDateTime.now());

    return interest;
  }
}
