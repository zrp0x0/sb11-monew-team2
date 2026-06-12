package com.codeit.monew.domain.userActivity.controller;

import com.codeit.monew.domain.userActivity.dto.UserActivityDto;
import com.codeit.monew.domain.userActivity.service.UserActivityService;
import io.swagger.v3.oas.annotations.Operation;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/user-activities")
public class UserActivityController {

    private final UserActivityService userActivityService;

    @Operation(summary = "사용자 활동 내역 조회", description = "사용자 ID로 활동 내역을 조회합니다.")
    @GetMapping("/{userId}")
    public UserActivityDto getUserActivity(
        @PathVariable UUID userId
    ) {
        log.info("사용자 활동 내역 조회 요청. userId: {}", userId);
        return userActivityService.get(userId);
    }
}
