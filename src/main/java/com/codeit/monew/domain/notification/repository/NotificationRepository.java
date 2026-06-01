package com.codeit.monew.domain.notification.repository;

import com.codeit.monew.domain.notification.entity.Notification;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<Notification, UUID>,
    NotificationRepositoryCustom {

    // 벌크 연산 처리
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Notification n SET n.confirmed = true, n.updatedAt = :updatedAt WHERE n.user.id = :userId AND n.confirmed = false")
    int confirmAllByUserId(@Param("userId") UUID userId,
        @Param("updatedAt") LocalDateTime updatedAt);
}
