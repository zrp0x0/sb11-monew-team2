package com.codeit.monew.domain.interest.service;

import com.codeit.monew.domain.interest.dto.request.InterestRegisterRequest;
import com.codeit.monew.domain.interest.dto.request.InterestUpdateRequest;
import com.codeit.monew.domain.interest.dto.response.InterestResponse;
import com.codeit.monew.domain.interest.entity.Interest;
import com.codeit.monew.domain.interest.exception.InterestErrorCode;
import com.codeit.monew.domain.interest.exception.InterestException;
import com.codeit.monew.domain.interest.repository.InterestRepository;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.text.similarity.LevenshteinDistance;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class InterestService {

  private final InterestRepository interestRepository;

  private static final int LENGTH_MARGIN = 2; // +- 2글자까지만 비교 허용

  @Transactional
  public InterestResponse create(InterestRegisterRequest request) {
    log.debug("interest register 시작 - 입력값: {}", request);
    String name = request.name();

    if(interestRepository.existsByName(name)) {
      throw new InterestException(InterestErrorCode.INTEREST_ALREADY_EXISTS, Map.of("name", name));
    }

    checkOptimizedSimilarity(name);

    Interest interest = Interest.create(name, request.keywords());
    interest = interestRepository.save(interest);
    log.info("interest register 완료");

    return InterestResponse.from(interest);
  }

  @Transactional
  public InterestResponse update(UUID interestId, InterestUpdateRequest request) {
    log.debug("interest update 시작 - 입력값: {}, {}", interestId, request);
    Interest interest = interestRepository.findById(interestId)
        .orElseThrow(() -> new InterestException(InterestErrorCode.INTEREST_NOT_FOUND, Map.of("interestId", interestId)));

    interest.updateKeywords(request.keywords());
    log.info("interest update 완료 - interestId: {}", interestId);

    return InterestResponse.from(interest);
  }

  // Levenshtein 알고리즘을 활용한 유사도 검증 헬퍼 메서드
  private void checkOptimizedSimilarity(String newName) {
    int len = newName.length();

    int minLength = Math.max(1, len - LENGTH_MARGIN);
    int maxLength = len + LENGTH_MARGIN;

    List<Interest> candidates = interestRepository.findSimilarLengthInterests(minLength, maxLength);

    if (candidates.isEmpty()) return;

    LevenshteinDistance levenshteinDistance = new LevenshteinDistance();

    for (Interest existing : candidates) {
      String existingName = existing.getName();

      int distance = levenshteinDistance.apply(newName, existingName);
      int maxLen = Math.max(len, existingName.length());

      double similarity = 1.0 - ((double) distance / maxLen);

      double threshold = (maxLen <= 3) ? 0.6 : 0.8; // 글자가 3글자보다 작으면 60%, 크면 80%로 유사도 기준 설정

      if (similarity >= threshold) {
        log.warn("유사도 충돌 - 요청: {}, 기존: {}, 임계값: {}, 유사도: {}", newName, existingName, threshold, similarity);
        throw new InterestException(InterestErrorCode.SIMILAR_INTEREST_EXISTS, Map.of(
            "requestedName", newName,
            "similarName", existingName,
            "threshold", threshold,
            "similarity", Math.round(similarity * 100) + "%"
        ));
      }
    }
  }
}
