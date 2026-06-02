package com.codeit.monew.domain.notification.service;

import com.codeit.monew.domain.notification.dto.NotificationDto;
import com.codeit.monew.domain.notification.dto.NotificationSearchCondition;
import com.codeit.monew.domain.notification.entity.Notification;
import com.codeit.monew.domain.notification.exception.NotificationErrorCode;
import com.codeit.monew.domain.notification.exception.NotificationException;
import com.codeit.monew.domain.notification.listener.NotificationCreateEvent;
import com.codeit.monew.domain.notification.repository.NotificationRepository;
import com.codeit.monew.domain.user.entity.User;
import com.codeit.monew.domain.user.exception.UserErrorCode;
import com.codeit.monew.domain.user.exception.UserException;
import com.codeit.monew.domain.user.repository.UserRepository;
import com.codeit.monew.global.dto.CursorPageResponse;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
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
     * 알림 목록 조회
     */
    public CursorPageResponse<NotificationDto> getNotifications(
        NotificationSearchCondition condition, UUID requestUserId) {
        CursorPageResponse<Notification> pageResponse = notificationRepository.searchNotifications(
            condition, requestUserId);
        List<NotificationDto> notifications = pageResponse.content().stream()
            .map(NotificationDto::from)
            .toList();

        log.info("미확인 알림 목록 조회 요청 성공.");
        return new CursorPageResponse<>(
            notifications,
            pageResponse.nextCursor(),
            pageResponse.nextAfter(),
            pageResponse.size(),
            pageResponse.totalElements(),
            pageResponse.hasNext()
        );
    }

    /**
     * 전체 알림 확인
     */
    @Transactional
    public void confirmAllNotification(UUID requestUserId) {
        userRepository.findById(requestUserId)
            .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND));

        // 알림 일괄 삭제
        int rowCount = notificationRepository.confirmAllByUserId(requestUserId,
            LocalDateTime.now());
        log.info("전체 알림 {}건 확인 요청 완료. UserId: {}", rowCount, requestUserId);
    }

    /**
     * 알림 확인
     */
    @Transactional
    public void confirmNotification(UUID notificationId, UUID requestUserId) {
        Notification foundNotification = notificationRepository.findById(notificationId)
            .orElseThrow(
                () -> new NotificationException(NotificationErrorCode.NOTIFICATION_NOT_FOUND));

        userRepository.findById(requestUserId)
            .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND));

        foundNotification.confirm();

        log.info("알림 확인 요청 완료. UserId: {}", requestUserId);
    }

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
