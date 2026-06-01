package com.codeit.monew.domain.notification.dto;

import com.codeit.monew.domain.notification.entity.Notification;
import com.codeit.monew.domain.notification.entity.NotificationResourceType;
import java.time.LocalDateTime;
import java.util.UUID;

public record NotificationDto(
    UUID id,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    boolean confirmed,
    UUID userId,
    String content,
    NotificationResourceType resourceType,
    UUID resourceId
) {

    public static NotificationDto from(Notification notification) {
        return new NotificationDto(
            notification.getId(),
            notification.getCreatedAt(),
            notification.getUpdatedAt(),
            notification.isConfirmed(),
            notification.getUser().getId(),
            notification.getContent(),
            notification.getResourceType(),
            notification.getResourceId()
        );
    }
}