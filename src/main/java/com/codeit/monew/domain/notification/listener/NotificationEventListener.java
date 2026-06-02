package com.codeit.monew.domain.notification.listener;

import static com.codeit.monew.global.config.AsyncConfig.NotificationAsyncName;

import com.codeit.monew.domain.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final NotificationService notificationService;

    @Async(NotificationAsyncName)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleNotificationCreateEvent(NotificationCreateEvent event) {
        log.info("[Async Event Received] 알림 생성 시작. 수신자 ID: {}, 리소스: {}", event.receiverId(),
            event.resourceType());

        try {
            // 별도의 스레드에서 실행되므로 예외가 발생해도 메인 비즈니스 로직에 영향을 주지 않음
            notificationService.createNotification(event);
        } catch (Exception e) {
            // 비동기 스레드 내의 예외는 밖으로 던져지지 않으므로, 반드시 내부에서 로깅해야 추적이 가능함
            log.error("[Async Event Error] 알림 생성 실패. 수신자 ID: {}", event.receiverId(), e);
        }
    }
}