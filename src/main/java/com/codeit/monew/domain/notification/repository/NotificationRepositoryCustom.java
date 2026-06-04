package com.codeit.monew.domain.notification.repository;

import com.codeit.monew.domain.notification.dto.NotificationSearchCondition;
import com.codeit.monew.domain.notification.entity.Notification;
import com.codeit.monew.global.dto.CursorPageResponse;
import java.util.UUID;

public interface NotificationRepositoryCustom {

    CursorPageResponse<Notification> searchNotifications(NotificationSearchCondition condition,
        UUID requestUserId);
}
