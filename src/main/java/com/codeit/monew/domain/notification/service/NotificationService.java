package com.codeit.monew.domain.notification.service;

import com.codeit.monew.domain.notification.entity.Notification;
import com.codeit.monew.domain.notification.listener.NotificationCreateEvent;
import com.codeit.monew.domain.notification.repository.NotificationRepository;
import com.codeit.monew.domain.user.entity.User;
import com.codeit.monew.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    /**
     * 알림 이벤트 핸들러 처리 (알림 생성)
     */
    @Transactional
    public void createNotification(NotificationCreateEvent event) {
        // 수신자 검증: 회원이 탈퇴했거나 없는 경우 알림 생성 무시
        User receiver = userRepository.findById(event.receiverId())
            .orElse(null);

        if (receiver == null) {
            log.warn("[Notification] 수신자를 찾을 수 없거나 탈퇴한 회원입니다. receiverId: {}", event.receiverId());
            return;
        }

        // 알림 생성
        Notification newNotification = Notification.create(receiver, event.content(),
            event.resourceType(), event.resourceId());

        notificationRepository.save(newNotification);
        log.info("[Notification Created] 알림 저장 완료. notificationId: {}, receiverId: {}",
            newNotification.getId(), newNotification.getUser().getId());
    }
}
