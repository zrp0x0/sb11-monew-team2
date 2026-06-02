package com.codeit.monew.domain.userActivity.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.codeit.monew.domain.article.entity.ArticleSource;
import com.codeit.monew.domain.user.exception.UserErrorCode;
import com.codeit.monew.domain.user.exception.UserException;
import com.codeit.monew.domain.userActivity.dto.UserActivityArticleViewDto;
import com.codeit.monew.domain.userActivity.dto.UserActivityCommentDto;
import com.codeit.monew.domain.userActivity.dto.UserActivityCommentLikeDto;
import com.codeit.monew.domain.userActivity.dto.UserActivityDto;
import com.codeit.monew.domain.userActivity.dto.UserActivitySubscriptionDto;
import com.codeit.monew.domain.userActivity.service.UserActivityService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(UserActivityController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserActivityControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    UserActivityService userActivityService;

    @Test
    @DisplayName("사용자 활동 내역 조회 성공 시 UserActivityDto를 반환")
    void getUserActivity_success() throws Exception {
        // given
        UUID userId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UserActivityDto response = createUserActivity(userId);

        when(userActivityService.get(eq(userId), eq(userId.toString()))).thenReturn(response);

        // when & then
        mockMvc.perform(get("/api/user-activities/{userId}", userId)
                        .header("Monew-Request-User-ID", userId.toString()))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.userId").value(userId.toString()))
                .andExpect(jsonPath("$.subscriptions[0].interestName").value("interest"))
                .andExpect(jsonPath("$.subscriptions[0].interestKeywords[0]").value("keyword"))
                .andExpect(jsonPath("$.comments[0].content").value("comment content"))
                .andExpect(jsonPath("$.commentLikes[0].commentContent").value("liked comment"))
                .andExpect(jsonPath("$.articleViews[0].source").value("NAVER"))
                .andExpect(jsonPath("$.articleViews[0].articleTitle").value("article title"));
    }

    @Test
    @DisplayName("요청자 헤더가 없으면 401 Unauthorized를 반환")
    void getUserActivity_fail_whenRequestUserHeaderMissing() throws Exception {
        // given
        UUID userId = UUID.fromString("11111111-1111-1111-1111-111111111111");

        when(userActivityService.get(eq(userId), isNull()))
                .thenThrow(new UserException(UserErrorCode.REQUEST_USER_ID_REQUIRED));

        // when & then
        mockMvc.perform(get("/api/user-activities/{userId}", userId))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("REQUEST_USER_ID_REQUIRED"));
    }

    @Test
    @DisplayName("다른 사용자의 활동 내역을 조회하면 403 Forbidden을 반환")
    void getUserActivity_fail_whenRequestUserMismatched() throws Exception {
        // given
        UUID userId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID requestUserId = UUID.fromString("22222222-2222-2222-2222-222222222222");

        when(userActivityService.get(eq(userId), eq(requestUserId.toString())))
                .thenThrow(new UserException(UserErrorCode.USER_ACCESS_DENIED));

        // when & then
        mockMvc.perform(get("/api/user-activities/{userId}", userId)
                        .header("Monew-Request-User-ID", requestUserId.toString()))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.code").value("USER_ACCESS_DENIED"));
    }

    @Test
    @DisplayName("조회 대상 사용자가 없으면 404 Not Found를 반환")
    void getUserActivity_fail_whenUserNotFound() throws Exception {
        // given
        UUID userId = UUID.fromString("11111111-1111-1111-1111-111111111111");

        when(userActivityService.get(eq(userId), eq(userId.toString())))
                .thenThrow(new UserException(UserErrorCode.USER_NOT_FOUND));

        // when & then
        mockMvc.perform(get("/api/user-activities/{userId}", userId)
                        .header("Monew-Request-User-ID", userId.toString()))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"));
    }

    private UserActivityDto createUserActivity(UUID userId) {
        return new UserActivityDto(
                userId,
                List.of(new UserActivitySubscriptionDto(
                        UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"),
                        UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"),
                        "interest",
                        List.of("keyword"),
                        1L,
                        LocalDateTime.of(2026, 6, 1, 10, 0)
                )),
                List.of(new UserActivityCommentDto(
                        UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc"),
                        UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd"),
                        "comment article",
                        userId,
                        "nickname",
                        "comment content",
                        3,
                        LocalDateTime.of(2026, 6, 1, 11, 0)
                )),
                List.of(new UserActivityCommentLikeDto(
                        UUID.fromString("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee"),
                        UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff"),
                        UUID.fromString("99999999-9999-9999-9999-999999999999"),
                        "liked article",
                        UUID.fromString("88888888-8888-8888-8888-888888888888"),
                        "writer",
                        "liked comment",
                        5,
                        LocalDateTime.of(2026, 6, 1, 12, 0),
                        LocalDateTime.of(2026, 6, 1, 12, 30)
                )),
                List.of(new UserActivityArticleViewDto(
                        UUID.fromString("77777777-7777-7777-7777-777777777777"),
                        UUID.fromString("66666666-6666-6666-6666-666666666666"),
                        ArticleSource.NAVER,
                        "https://news.example.com/article",
                        "article title",
                        LocalDateTime.of(2026, 6, 1, 13, 0),
                        "article summary",
                        4L,
                        10L,
                        LocalDateTime.of(2026, 6, 1, 13, 30)
                ))
        );
    }
}
