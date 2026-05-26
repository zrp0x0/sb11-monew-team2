package com.codeit.monew.domain.subscription.controller;

import static com.codeit.monew.global.filter.MDCLoggingFilter.HEADER_USER_ID;

import com.codeit.monew.domain.subscription.dto.response.SubscriptionDto;
import com.codeit.monew.domain.subscription.service.SubscriptionService;
import io.swagger.v3.oas.annotations.Operation;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

// TODO: InterestController랑 통합

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/interests")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    @Operation(summary = "관심사 구독", description = "관심사를 구독합니다.")
    @PostMapping("/{interestId}/subscriptions")
    @ResponseStatus(HttpStatus.OK)
    public SubscriptionDto createSubscription(
        @PathVariable UUID interestId,
        @RequestHeader(HEADER_USER_ID) UUID requestUserId
    ) {
        return subscriptionService.createSubscription(interestId, requestUserId);
    }

}
