package com.codeit.monew.global.monitoring.repository;

import com.codeit.monew.global.monitoring.domain.JobRunHistory;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JobRunHistoryRepository extends JpaRepository<JobRunHistory, UUID> {

  Optional<JobRunHistory> findTopByJobNameOrderByStartedAtDesc(String jobName);

  List<JobRunHistory> findTop20ByJobNameOrderByStartedAtDesc(String jobName);
}
