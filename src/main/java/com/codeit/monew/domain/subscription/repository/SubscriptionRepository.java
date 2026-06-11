package com.codeit.monew.domain.subscription.repository;

import com.codeit.monew.domain.subscription.entity.Subscription;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {

  @Query("SELECT s FROM Subscription s JOIN FETCH s.interest WHERE s.interest.id = :interestId AND s.user.id = :userId")
  Optional<Subscription> findByInterestIdAndUserIdWithInterest(
      @Param("interestId") UUID interestId, @Param("userId") UUID userId);

  Optional<Subscription> findByInterestIdAndUserId(UUID interestId, UUID userId);

  @Query("SELECT s.interest.id FROM Subscription s " +
      "WHERE s.user.id = :userId AND s.interest.id IN :interestIds")
  Set<UUID> findSubscribedInterestIds(
      @Param("userId") UUID userId, @Param("interestIds") List<UUID> interestIds);

  @Modifying(clearAutomatically = true)
  @Query("DELETE FROM Subscription s WHERE s.interest.id = :interestId")
  void deleteByInterestId(@Param("interestId") UUID interestId);

  // 특정 관심사를 구독 중인 모든 유저의 ID 목록만 조회함
  @Query("SELECT s.user.id FROM Subscription s WHERE s.interest.id = :interestId")
  List<UUID> findUserIdsByInterestId(@Param("interestId") UUID interestId);

  @Modifying(clearAutomatically = true)
  @Query("DELETE FROM Subscription s WHERE s.interest.id = :interestId AND s.user.id = :userId")
  int deleteByInterestIdAndUserId(@Param("interestId") UUID interestId, @Param("userId") UUID userId);
}
