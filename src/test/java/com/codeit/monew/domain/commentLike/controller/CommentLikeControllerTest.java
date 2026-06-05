package com.codeit.monew.domain.commentLike.controller;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.codeit.monew.domain.commentLike.dto.CommentLikeDto;
import com.codeit.monew.domain.commentLike.exception.CommentLikeErrorCode;
import com.codeit.monew.domain.commentLike.exception.CommentLikeException;
import com.codeit.monew.domain.commentLike.service.CommentLikeService;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CommentLikeController.class)
@AutoConfigureMockMvc(addFilters = false)
class CommentLikeControllerTest {

    private static final UUID COMMENT_ID = UUID.randomUUID();
    private static final UUID REQUEST_USER_ID = UUID.randomUUID();
    private static final UUID ARTICLE_ID = UUID.randomUUID();
    private static final UUID COMMENT_USER_ID = UUID.randomUUID();
    private static final UUID COMMENT_LIKE_ID = UUID.randomUUID();
    private static final LocalDateTime CREATED_AT = LocalDateTime.of(2026, 6, 1, 12, 0);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CommentLikeService commentLikeService;

    @Nested
    @DisplayName("댓글 좋아요 등록 - POST")
    class Create {

        @Test
        @DisplayName("정상 요청 시 200과 CommentLikeDto를 반환한다")
        void create_success() throws Exception {
            CommentLikeDto response = new CommentLikeDto(
                    COMMENT_LIKE_ID,
                    REQUEST_USER_ID,
                    CREATED_AT,
                    COMMENT_ID,
                    ARTICLE_ID,
                    COMMENT_USER_ID,
                    "commenter",
                    "좋은 댓글",
                    3,
                    CREATED_AT.minusHours(1)
            );
            given(commentLikeService.create(COMMENT_ID, REQUEST_USER_ID)).willReturn(response);

            mockMvc.perform(post("/api/comments/{commentId}/comment-likes", COMMENT_ID)
                            .header("Monew-Request-User-ID", REQUEST_USER_ID.toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(COMMENT_LIKE_ID.toString()))
                    .andExpect(jsonPath("$.likedBy").value(REQUEST_USER_ID.toString()))
                    .andExpect(jsonPath("$.commentId").value(COMMENT_ID.toString()))
                    .andExpect(jsonPath("$.articleId").value(ARTICLE_ID.toString()))
                    .andExpect(jsonPath("$.commentUserId").value(COMMENT_USER_ID.toString()))
                    .andExpect(jsonPath("$.commentUserNickname").value("commenter"))
                    .andExpect(jsonPath("$.commentContent").value("좋은 댓글"))
                    .andExpect(jsonPath("$.commentLikeCount").value(3));

            verify(commentLikeService).create(COMMENT_ID, REQUEST_USER_ID);
        }

        @Test
        @DisplayName("요청자 헤더가 없으면 400을 반환한다")
        void create_missingRequestUserHeader_returns400() throws Exception {
            mockMvc.perform(post("/api/comments/{commentId}/comment-likes", COMMENT_ID))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("MISSING_REQUEST_HEADER"));
        }

        @Test
        @DisplayName("commentId가 UUID 형식이 아니면 400을 반환한다")
        void create_invalidCommentId_returns400() throws Exception {
            mockMvc.perform(post("/api/comments/{commentId}/comment-likes", "not-a-uuid")
                            .header("Monew-Request-User-ID", REQUEST_USER_ID.toString()))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("INVALID_UUID_FORMAT"));
        }

        @Test
        @DisplayName("이미 좋아요한 댓글이면 400을 반환한다")
        void create_alreadyExists_returns400() throws Exception {
            given(commentLikeService.create(COMMENT_ID, REQUEST_USER_ID))
                    .willThrow(new CommentLikeException(CommentLikeErrorCode.COMMENT_LIKE_ALREADY_EXISTS));

            mockMvc.perform(post("/api/comments/{commentId}/comment-likes", COMMENT_ID)
                            .header("Monew-Request-User-ID", REQUEST_USER_ID.toString()))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("COMMENT_LIKE_ALREADY_EXISTS"));
        }
    }

    @Nested
    @DisplayName("댓글 좋아요 취소 - DELETE")
    class Delete {

        @Test
        @DisplayName("정상 요청 시 200을 반환한다")
        void delete_success() throws Exception {
            willDoNothing().given(commentLikeService).delete(COMMENT_ID, REQUEST_USER_ID);

            mockMvc.perform(delete("/api/comments/{commentId}/comment-likes", COMMENT_ID)
                            .header("Monew-Request-User-ID", REQUEST_USER_ID.toString()))
                    .andExpect(status().isOk());

            verify(commentLikeService).delete(COMMENT_ID, REQUEST_USER_ID);
        }

        @Test
        @DisplayName("좋아요가 없으면 404를 반환한다")
        void delete_notFound_returns404() throws Exception {
            willThrow(new CommentLikeException(CommentLikeErrorCode.COMMENT_LIKE_NOT_FOUND))
                    .given(commentLikeService)
                    .delete(COMMENT_ID, REQUEST_USER_ID);

            mockMvc.perform(delete("/api/comments/{commentId}/comment-likes", COMMENT_ID)
                            .header("Monew-Request-User-ID", REQUEST_USER_ID.toString()))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("COMMENT_LIKE_NOT_FOUND"));
        }

        @Test
        @DisplayName("요청자 헤더 UUID 형식이 아니면 400을 반환한다")
        void delete_invalidRequestUserId_returns400() throws Exception {
            mockMvc.perform(delete("/api/comments/{commentId}/comment-likes", COMMENT_ID)
                            .header("Monew-Request-User-ID", "invalid-user-id"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("INVALID_UUID_FORMAT"));
        }
    }
}
