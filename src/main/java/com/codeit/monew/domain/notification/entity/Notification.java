package com.codeit.monew.domain.notification.entity;

import com.codeit.monew.domain.user.entity.User;
import com.codeit.monew.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "notifications")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "notification_id")
    private UUID id;

    @Column(nullable = false)
    private boolean confirmed = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "resource_type", nullable = false)
    private NotificationResourceType resourceType;

    @Column(name = "resource_id", nullable = false)
    private UUID resourceId;

    private Notification(User user, String content, NotificationResourceType resourceType,
        UUID resourceId) {
        this.user = user;
        this.content = content;
        this.resourceType = resourceType;
        this.resourceId = resourceId;
    }

    // == 생성 메서드 ==
    public static Notification create(User user, String content,
        NotificationResourceType resourceType, UUID resourceId) {
        return new Notification(
            user,
            content,
            resourceType,
            resourceId
        );
    }

    // == 비즈니스 메서드 ==

    /**
     * 알림 확인
     */
    public void confirm() {
        if (!this.confirmed) {
            this.confirmed = true;
        }
    }
}
