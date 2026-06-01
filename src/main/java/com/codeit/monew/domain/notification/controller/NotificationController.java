package com.codeit.monew.domain.notification.controller;

import static com.codeit.monew.global.filter.MDCLoggingFilter.HEADER_USER_ID;

import com.codeit.monew.domain.notification.dto.NotificationDto;
import com.codeit.monew.domain.notification.dto.NotificationSearchCondition;
import com.codeit.monew.domain.notification.service.NotificationService;
import com.codeit.monew.global.dto.CursorPageResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    @Operation(summary = "알림 목록 조회", description = "알림 목록을 조회합니다.")
    @GetMapping()
    @ResponseStatus(HttpStatus.OK)
    public CursorPageResponse<NotificationDto> getNotifications(
        @Valid @ModelAttribute NotificationSearchCondition condition,
        @RequestHeader(HEADER_USER_ID) UUID requestUserId
    ) {
        return notificationService.getNotifications(condition, requestUserId);
    }

    @Operation(summary = "전체 알림 확인", description = "전체 알림을 한번에 확인합니다.")
    @PatchMapping()
    @ResponseStatus(HttpStatus.OK)
    public void confirmAllNotifications(@RequestHeader(HEADER_USER_ID) UUID requestUserId) {
        notificationService.confirmAllNotification(requestUserId);
    }

    @Operation(summary = "알림 확인", description = "알림을 확인합니다.")
    @PatchMapping("/{notificationId}")
    @ResponseStatus(HttpStatus.OK)
    public void confirmNotification(
        @PathVariable UUID notificationId,
        @RequestHeader(HEADER_USER_ID) UUID requestUserId
    ) {
        notificationService.confirmNotification(notificationId, requestUserId);
    }
}
