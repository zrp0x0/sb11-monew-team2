package com.codeit.monew.domain.subscription.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.codeit.monew.domain.subscription.dto.response.SubscriptionDto;
import com.codeit.monew.domain.subscription.service.SubscriptionService;
import com.codeit.monew.global.filter.MDCLoggingFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(SubscriptionController.class)
@AutoConfigureMockMvc(addFilters = false)
class SubscriptionControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockitoBean
  private SubscriptionService subscriptionService;

  @Test
  @DisplayName("관심사 구독 생성 요청이 성공하면 200 상태코드와 구독 정보를 반환한다")
  void createSubscription_Success() throws Exception {
    // given
    UUID interestId = UUID.randomUUID();
    UUID requestUserId = UUID.randomUUID();
    UUID subscriptionId = UUID.randomUUID();
    LocalDateTime createdAt = LocalDateTime.of(2026, 6, 8, 10, 0);

    SubscriptionDto responseDto = new SubscriptionDto(
        subscriptionId,
        interestId,
        "테스트 관심사",
        List.of("키워드1", "키워드2"),
        10L,
        createdAt
    );

    when(subscriptionService.createSubscription(any(UUID.class), any(UUID.class)))
        .thenReturn(responseDto);

    // when & then
    mockMvc.perform(post("/api/interests/{interestId}/subscriptions", interestId)
            .header(MDCLoggingFilter.HEADER_USER_ID, requestUserId.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(subscriptionId.toString()))
        .andExpect(jsonPath("$.interestId").value(interestId.toString()))
        .andExpect(jsonPath("$.interestName").value("테스트 관심사"))
        .andExpect(jsonPath("$.interestKeywords[0]").value("키워드1"))
        .andExpect(jsonPath("$.interestKeywords[1]").value("키워드2"))
        .andExpect(jsonPath("$.interestSubscriberCount").value(10))
        .andExpect(jsonPath("$.createdAt").value("2026-06-08T10:00:00"))
        .andDo(print());

    verify(subscriptionService).createSubscription(interestId, requestUserId);
  }

  @Test
  @DisplayName("관심사 구독 취소 요청이 성공하면 200 상태코드를 반환한다")
  void cancelSubscription_Success() throws Exception {
    // given
    UUID interestId = UUID.randomUUID();
    UUID requestUserId = UUID.randomUUID();

    doNothing().when(subscriptionService).cancelSubscription(any(UUID.class), any(UUID.class));

    // when & then
    mockMvc.perform(delete("/api/interests/{interestId}/subscriptions", interestId)
            .header(MDCLoggingFilter.HEADER_USER_ID, requestUserId.toString()))
        .andExpect(status().isOk())
        .andDo(print());

    verify(subscriptionService).cancelSubscription(interestId, requestUserId);
  }

  @Test
  @DisplayName("요청 헤더에 사용자 ID가 없으면 구독 생성 요청은 400 에러를 반환한다")
  void createSubscription_Fail_MissingHeader() throws Exception {
    // given
    UUID interestId = UUID.randomUUID();

    // when & then
    mockMvc.perform(post("/api/interests/{interestId}/subscriptions", interestId))
        .andExpect(status().isBadRequest())
        .andDo(print());
  }

  @Test
  @DisplayName("요청 헤더에 사용자 ID가 없으면 구독 취소 요청은 400 에러를 반환한다")
  void cancelSubscription_Fail_MissingHeader() throws Exception {
    // given
    UUID interestId = UUID.randomUUID();

    // when & then
    mockMvc.perform(delete("/api/interests/{interestId}/subscriptions", interestId))
        .andExpect(status().isBadRequest())
        .andDo(print());
  }
}