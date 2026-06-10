package com.codeit.monew.global.monitoring.service;

import com.codeit.monew.global.monitoring.domain.JobRunHistory;
import com.codeit.monew.global.monitoring.repository.JobRunHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class JobRunHistoryService {

  private final JobRunHistoryRepository jobRunHistoryRepository;

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public JobRunHistory record(JobRunHistoryCommand command) {
    JobRunHistory history = JobRunHistory.create(
        command.jobName(),
        command.status(),
        command.startedAt(),
        command.endedAt(),
        command.durationMs(),
        command.targetDate(),
        command.totalCount(),
        command.successCount(),
        command.failedCount(),
        command.skippedCount(),
        command.message()
    );

    return jobRunHistoryRepository.save(history);
  }
}
