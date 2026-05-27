package com.codeit.monew.domain.interest.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;

import com.codeit.monew.domain.interest.dto.request.InterestRegisterRequest;
import com.codeit.monew.domain.interest.dto.request.InterestUpdateRequest;
import com.codeit.monew.domain.interest.dto.response.InterestResponse;
import com.codeit.monew.domain.interest.entity.Interest;
import com.codeit.monew.domain.interest.exception.InterestErrorCode;
import com.codeit.monew.domain.interest.exception.InterestException;
import com.codeit.monew.domain.interest.repository.InterestRepository;
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

  @InjectMocks
  private InterestService interestService;

  @Test
  @DisplayName("관심사 등록 성공")
  void createInterest_success() {
    //given
    InterestRegisterRequest request = new InterestRegisterRequest("스포츠", List.of("축구"));

    given(interestRepository.existsByName(request.name()))
        .willReturn(false);

    Interest interest = Interest.create("스포츠", List.of("축구"));
    ReflectionTestUtils.setField(interest, "id", UUID.randomUUID());

    given(interestRepository.save(any(Interest.class)))
        .willReturn(interest);

    //when
    InterestResponse result = interestService.create(request);

    //then
    assertThat(result).isNotNull();
    assertThat(result.name()).isEqualTo("스포츠");
    assertThat(result.keywords()).hasSize(1);
  }

  @Test
  @DisplayName("관심사 등록 실패: 중복된 관심사가 있을 시 INTEREST_ALREADY_EXISTS 예외 발생")
  void createInterest_fail_already_exists() {
    //given
    InterestRegisterRequest request = new InterestRegisterRequest("스포츠", List.of("체육"));

    given(interestRepository.existsByName(request.name()))
        .willReturn(true);

    //when & then
    assertThatThrownBy(() -> interestService.create(request))
        .isInstanceOf(InterestException.class)
        .extracting("errorCode").isEqualTo(InterestErrorCode.INTEREST_ALREADY_EXISTS);
  }

  @ParameterizedTest(name = "[{index}] 요청: {0}, 기존: {1}")
  @MethodSource("similaritySuccessCases")
  @DisplayName("관심사 등록 성공: 유사도가 기준치 미만이거나 길이가 확연히 다를 시 등록 성공")
  void createInterest_success_not_similar(String newName, String existingName) {
    //given
    InterestRegisterRequest request = new InterestRegisterRequest(newName, List.of("테스트"));
    Interest existingInterest = Interest.create(existingName, List.of("테스트"));

    given(interestRepository.existsByName(newName))
        .willReturn(false);
    given(interestRepository.findSimilarLengthInterests(anyInt(), anyInt()))
        .willReturn(List.of(existingInterest));

    Interest interest = Interest.create(newName, List.of("테스트"));
    ReflectionTestUtils.setField(interest, "id", UUID.randomUUID());

    given(interestRepository.save(any(Interest.class)))
        .willReturn(interest);

    //when & then
    assertDoesNotThrow(() -> interestService.create(request));
  }

  private static Stream<Arguments> similaritySuccessCases() {
    return Stream.of(
        Arguments.of("데이터분석", "데이터과학"),
        Arguments.of("축구", "농구")
    );
  }

  @ParameterizedTest(name = "[{index}] 요청: {0}, 기존: {1}")
  @MethodSource("similarityFailCases")
  @DisplayName("관심사 등록 실패: 관심사 유사도 기준 초과 시 SIMILAR_INTEREST_EXISTS 예외 발생")
  void createInterest_fail_similar_interest_exists(String newName, String existingName) {
    //given
    InterestRegisterRequest request = new InterestRegisterRequest(newName, List.of("테스트"));
    Interest existingInterest = Interest.create(existingName, List.of("테스트"));

    given(interestRepository.existsByName(request.name()))
        .willReturn(false);
    given(interestRepository.findSimilarLengthInterests(anyInt(), anyInt()))
        .willReturn(List.of(existingInterest));

    //when & then
    assertThatThrownBy(() -> interestService.create(request))
        .isInstanceOf(InterestException.class)
        .extracting("errorCode").isEqualTo(InterestErrorCode.SIMILAR_INTEREST_EXISTS);
  }

  private static Stream<Arguments> similarityFailCases() {
    return Stream.of(
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
    InterestResponse result = interestService.update(interestId, request);

    //then
    assertThat(result.id()).isEqualTo(interestId);
    assertThat(result.keywords()).doesNotContain("수영");
    assertThat(result.keywords()).hasSize(2);
  }

  @Test
  @DisplayName("관심사 키워드 수정 실패: 존재하지 않는 ID를 넘길 시 INTEREST_NOT_FOUND 예외 발생")
  void updateKeywords_fail_not_found() {
    //given
    UUID invalidId = UUID.randomUUID();
    InterestUpdateRequest request = new InterestUpdateRequest(List.of("새키워드"));

    given(interestRepository.findById(invalidId))
        .willReturn(Optional.empty());

    //when & then
    assertThatThrownBy(() -> interestService.update(invalidId, request))
        .isInstanceOf(InterestException.class)
        .extracting("errorCode").isEqualTo(InterestErrorCode.INTEREST_NOT_FOUND);
  }
}
