package com.codeit.monew.domain.interest.service;

import com.codeit.monew.domain.article.repository.ArticleInterestRepository;
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
import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.time.format.DateTimeFormatter;
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
  private final UserRepository userRepository;
  private final SubscriptionRepository subscriptionRepository;
  private final ArticleInterestRepository articleInterestRepository;

  @Transactional
  public InterestResponse createInterest(InterestRegisterRequest request) {
    log.debug("interest register 시작 - 입력값: {}", request);
    String name = request.name();

    if (interestRepository.existsByName(name)) {
      throw new InterestException(InterestErrorCode.INTEREST_ALREADY_EXISTS, Map.of("name", name));
    }

    checkOptimizedSimilarity(name);

    Interest interest = Interest.create(name, request.keywords());
    interest = interestRepository.save(interest);
    log.info("interest register 완료");

    return InterestResponse.from(interest);
  }

  @Transactional
  public InterestResponse updateInterest(UUID interestId, InterestUpdateRequest request) {
    log.debug("interest update 시작 - 입력값: {}, {}", interestId, request);
    Interest interest = interestRepository.findById(interestId)
        .orElseThrow(() -> new InterestException(InterestErrorCode.INTEREST_NOT_FOUND,
            Map.of("interestId", interestId)));

    interest.updateKeywords(request.keywords());
    log.info("interest update 완료 - interestId: {}", interestId);

    return InterestResponse.from(interest);
  }

  @Transactional
  public void deleteInterest(UUID interestId) {
    log.debug("interest delete 시작 - 입력값: {}", interestId);
    if(!interestRepository.existsById(interestId)) {
      throw new InterestException(InterestErrorCode.INTEREST_NOT_FOUND, Map.of("interestId", interestId));
    }

    subscriptionRepository.deleteByInterestId(interestId);
    articleInterestRepository.deleteByInterestId(interestId);
    interestRepository.deleteById(interestId);
    log.info("interest delete - interestId: {}", interestId);
  }

  @Transactional(readOnly = true)
  public CursorPageResponse<InterestResponse> searchInterest(UUID userId,
      InterestSearchRequest request) {
    log.debug("interest search 시작 - 입력값: {}, {}", userId, request);
    if (!userRepository.existsById(userId)) {
      throw new UserException(UserErrorCode.INVALID_CREDENTIALS, Map.of("userId", userId));
    }

    List<Interest> interestList = interestRepository.findAllByCondition(request);

    boolean hasNext = false;
    if (interestList.size() > request.getLimit()) {
      hasNext = true;
      interestList = interestList.subList(0, request.getLimit());
    }

    if (interestList.isEmpty()) {
      return new CursorPageResponse<>(List.of(), null, null, request.getLimit(), 0L, false);
    }

    List<UUID> interestIds = interestList.stream()
        .map(Interest::getId)
        .toList();

    List<UUID> subscribedInterestIds = subscriptionRepository.findSubscribedInterestIds(userId,
        interestIds);

    List<InterestResponse> content = interestList.stream()
        .map(interest -> {
          boolean subscribedByMe = subscribedInterestIds.contains(interest.getId());
          return InterestResponse.of(interest, subscribedByMe);
        })
        .toList();

    Interest lastInterest = interestList.get(interestList.size() - 1);

    String nextCursor = request.getOrderBy().equals("subscriberCount")
        ? lastInterest.getSubscriberCount() + "_" + lastInterest.getCreatedAt() + "_" +lastInterest.getId()
        : lastInterest.getName();

    String nextAfter = hasNext ? String.valueOf(lastInterest.getCreatedAt()) : null;
    log.debug("interest search 완료 - 응답 사이즈: {}, hasNext: {}", content.size(), hasNext);

    return new CursorPageResponse<InterestResponse>(
        content,
        hasNext ? nextCursor : null,
        nextAfter,
        request.getLimit(),
        0L,
        hasNext
    );
  }

  // Levenshtein 알고리즘을 활용한 유사도 검증 헬퍼 메서드
  private void checkOptimizedSimilarity(String newName) {
    int len = newName.length();
    double threshold = 0.8;

    // 80% 유사도를 만족할 수 있는 길이 동적 계산 로직 적용
    int calculatedMin = (int) Math.ceil(len * threshold);
    int calculatedMax = (int) Math.floor(len / threshold);

    int minLength = Math.max(1, calculatedMin - 1);
    int maxLength = Math.min(20, calculatedMax + 1);

    List<Interest> candidates = interestRepository.findSimilarLengthInterests(minLength, maxLength);

    if (candidates.isEmpty()) return;

    LevenshteinDistance levenshteinDistance = new LevenshteinDistance();

    // 새 단어를 자모 분해 처리
    String decomposedNewName = Normalizer.normalize(newName, Form.NFD);

    for (Interest existing : candidates) {
      String existingName = existing.getName();

      // 기존 단어도 자모 분해 처리
      String decomposedExistingName = Normalizer.normalize(existingName, Form.NFD);

      // 쪼개진 모음/자음 상태로 거리 계산
      int distance = levenshteinDistance.apply(decomposedNewName, decomposedExistingName);
      int maxLen = Math.max(decomposedNewName.length(), decomposedExistingName.length());

      double similarity = 1.0 - ((double) distance / maxLen);

      if (similarity >= threshold) {
        log.warn("유사도 충돌 - 요청: {}, 기존: {}, 임계값: {}, 유사도: {}", newName, existingName, threshold,
            similarity);
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