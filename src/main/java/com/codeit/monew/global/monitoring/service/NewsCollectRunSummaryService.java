package com.codeit.monew.global.monitoring.service;

import com.codeit.monew.global.monitoring.domain.NewsCollectRunSummary;
import com.codeit.monew.global.monitoring.repository.NewsCollectRunSummaryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NewsCollectRunSummaryService {

  private final NewsCollectRunSummaryRepository newsCollectRunSummaryRepository;

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public NewsCollectRunSummary record(NewsCollectRunSummaryCommand command) {
    NewsCollectRunSummary summary = NewsCollectRunSummary.create(
        command.jobRunHistoryId(),
        command.interestCount(),
        command.apiCallCount(),
        command.candidateCount(),
        command.uniqueCandidateCount(),
        command.duplicateCount(),
        command.savedCount(),
        command.failedCount(),
        command.durationMs()
    );

    return newsCollectRunSummaryRepository.save(summary);
  }
}
