package com.codeit.monew.global.monitoring.repository;

import com.codeit.monew.global.monitoring.domain.NewsCollectRunSummary;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NewsCollectRunSummaryRepository extends JpaRepository<NewsCollectRunSummary, UUID> {
}
