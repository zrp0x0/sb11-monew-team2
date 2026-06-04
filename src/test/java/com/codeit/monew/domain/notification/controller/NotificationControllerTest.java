package com.codeit.monew.domain.notification.controller;

import static com.codeit.monew.global.filter.MDCLoggingFilter.HEADER_USER_ID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.codeit.monew.domain.notification.dto.NotificationDto;
import com.codeit.monew.domain.notification.dto.NotificationSearchCondition;
import com.codeit.monew.domain.notification.entity.NotificationResourceType;
import com.codeit.monew.domain.notification.service.NotificationService;
import com.codeit.monew.global.dto.CursorPageResponse;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(NotificationController.class)
@AutoConfigureMockMvc(addFilters = false)
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NotificationService notificationService;

    @Nested
    @DisplayName("GET /api/notifications - 알림 목록 조회")
    class GetNotificationsTest {

        @Test
        @DisplayName("성공: 유효한 파라미터와 헤더가 주어지면 200 OK와 알림 목록을 반환한다.")
        void getNotifications_success() throws Exception {
            // given
            UUID userId = UUID.randomUUID();
            NotificationDto dto = new NotificationDto(
                UUID.randomUUID(), LocalDateTime.now(), LocalDateTime.now(),
                false, userId, "새로운 알림입니다.", NotificationResourceType.ARTICLE, UUID.randomUUID()
            );
            CursorPageResponse<NotificationDto> mockResponse = new CursorPageResponse<>(
                List.of(dto), "nextCursor", "nextAfter", 10, 1L, false
            );

            given(notificationService.getNotifications(any(NotificationSearchCondition.class),
                eq(userId)))
                .willReturn(mockResponse);

            // when & then
            mockMvc.perform(get("/api/notifications")
                    .header(HEADER_USER_ID, userId.toString())
                    .param("limit", "10")
                    .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].content").value("새로운 알림입니다."))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.hasNext").value(false));
        }

        @Test
        @DisplayName("실패: 필수 헤더(User ID)가 누락되면 400 Bad Request를 반환한다.")
        void getNotifications_fail_missingHeader() throws Exception {
            // when & then
            mockMvc.perform(get("/api/notifications")
                    // .header(HEADER_USER_ID, ...) 누락
                    .param("limit", "10")
                    .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("실패: limit 조건이 유효하지 않으면(@Min 검증 실패) 400 Bad Request를 반환한다.")
        void getNotifications_fail_invalidLimit() throws Exception {
            // given
            UUID userId = UUID.randomUUID();

            // when & then
            mockMvc.perform(get("/api/notifications")
                    .header(HEADER_USER_ID, userId.toString())
                    .param("limit", "0") // 유효하지 않은 limit (1 미만)
                    .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("PATCH /api/notifications - 전체 알림 확인")
    class ConfirmAllNotificationsTest {

        @Test
        @DisplayName("성공: 유효한 헤더가 주어지면 200 OK를 반환하고 Service 메서드를 호출한다.")
        void confirmAllNotifications_success() throws Exception {
            // given
            UUID userId = UUID.randomUUID();

            // when & then
            mockMvc.perform(patch("/api/notifications")
                    .header(HEADER_USER_ID, userId.toString()))
                .andDo(print())
                .andExpect(status().isOk());

            // Service의 로직이 정확한 파라미터로 호출되었는지 검증
            verify(notificationService).confirmAllNotification(userId);
        }
    }

    @Nested
    @DisplayName("PATCH /api/notifications/{notificationId} - 단건 알림 확인")
    class ConfirmNotificationTest {

        @Test
        @DisplayName("성공: 유효한 경로 변수와 헤더가 주어지면 200 OK를 반환하고 Service 메서드를 호출한다.")
        void confirmNotification_success() throws Exception {
            // given
            UUID userId = UUID.randomUUID();
            UUID notificationId = UUID.randomUUID();

            // when & then
            mockMvc.perform(patch("/api/notifications/{notificationId}", notificationId.toString())
                    .header(HEADER_USER_ID, userId.toString()))
                .andDo(print())
                .andExpect(status().isOk());

            // Service의 로직이 정확한 파라미터로 호출되었는지 검증
            verify(notificationService).confirmNotification(notificationId, userId);
        }

        @Test
        @DisplayName("실패: 경로 변수가 올바른 UUID 형식이 아니면 400 Bad Request를 반환한다.")
        void confirmNotification_fail_invalidUUID() throws Exception {
            // given
            UUID userId = UUID.randomUUID();
            String invalidNotificationId = "invalid-uuid-string";

            // when & then
            mockMvc.perform(patch("/api/notifications/{notificationId}", invalidNotificationId)
                    .header(HEADER_USER_ID, userId.toString()))
                .andDo(print())
                .andExpect(status().isBadRequest());
        }
    }
}