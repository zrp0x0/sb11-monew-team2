package com.codeit.monew.domain.interest.service;

import com.codeit.monew.domain.interest.dto.request.InterestUpdateRequest;
import com.codeit.monew.domain.interest.dto.response.InterestResponse;
import com.codeit.monew.domain.interest.entity.Interest;
import com.codeit.monew.domain.interest.exception.InterestErrorCode;
import com.codeit.monew.domain.interest.exception.InterestException;
import com.codeit.monew.domain.interest.repository.InterestRepository;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class InterestService {

  private final InterestRepository interestRepository;

  @Transactional
  public InterestResponse update(UUID interestId, InterestUpdateRequest request) {
    log.debug("interest update 시작 - 입력값: {}, {}", interestId, request);
    Interest interest = interestRepository.findById(interestId)
        .orElseThrow(() -> new InterestException(InterestErrorCode.INTEREST_NOT_FOUND, Map.of("interestId", interestId)));

    interest.updateKeywords(request.keywords());
    log.info("interest update 완료 - interestId: {}", interestId);

    return InterestResponse.from(interest, false);
  }
}
