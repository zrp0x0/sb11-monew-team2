package com.codeit.monew.domain.subscription.repository;

import com.codeit.monew.domain.subscription.entity.Subscription;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {

    @Query("SELECT s FROM Subscription s JOIN FETCH s.interest WHERE s.interest.id = :interestId AND s.user.id = :userId")
    Optional<Subscription> findByInterestIdAndUserIdWithInterest(
        @Param("interestId") UUID interestId, @Param("userId") UUID userId);

    Optional<Subscription> findByInterestIdAndUserId(UUID interestId, UUID userId);
}
