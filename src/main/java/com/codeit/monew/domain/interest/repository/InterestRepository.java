package com.codeit.monew.domain.interest.repository;

import com.codeit.monew.domain.interest.entity.Interest;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InterestRepository extends JpaRepository<Interest, UUID>, InterestRepositoryCustom {

  boolean existsByName(String name);

  @Query("SELECT DISTINCT i FROM Interest i LEFT JOIN FETCH i.keywords")
  List<Interest> findAllWithKeywords();

  @Query("SELECT i FROM Interest i WHERE LENGTH(i.name) BETWEEN :minLength AND :maxLength")
  List<Interest> findSimilarLengthInterests(
      @Param("minLength") int minLength, @Param("maxLength") int maxLength);

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query("UPDATE Interest i SET i.subscriberCount = i.subscriberCount + 1 WHERE i.id = :id")
  int incrementSubscriberCount(@Param("id") UUID id);

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query("UPDATE Interest i SET i.subscriberCount = i.subscriberCount - 1 WHERE i.id = :id AND i.subscriberCount > 0")
  int decrementSubscriberCount(@Param("id") UUID id);
}
