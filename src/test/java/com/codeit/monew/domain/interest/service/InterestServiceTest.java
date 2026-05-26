package com.codeit.monew.domain.interest.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.codeit.monew.domain.interest.dto.request.InterestUpdateRequest;
import com.codeit.monew.domain.interest.dto.response.InterestResponse;
import com.codeit.monew.domain.interest.entity.Interest;
import com.codeit.monew.domain.interest.exception.InterestErrorCode;
import com.codeit.monew.domain.interest.exception.InterestException;
import com.codeit.monew.domain.interest.repository.InterestRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
  @DisplayName("관심사 키워드 수정 실패: 존재하지 않는 ID를 넘기면 INTEREST_NOT_FOUND 예외가 발생한다.")
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
