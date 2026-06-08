package com.codeit.monew.domain.comment.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.codeit.monew.domain.comment.dto.CommentDto;
import com.codeit.monew.domain.comment.dto.CommentRegisterRequest;
import com.codeit.monew.domain.comment.dto.CommentSearchRequest;
import com.codeit.monew.domain.comment.dto.CommentUpdateRequest;
import com.codeit.monew.domain.comment.dto.CursorPageResponseCommentDto;
import com.codeit.monew.domain.comment.service.CommentService;
import com.fasterxml.jackson.databind.ObjectMapper;
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

@WebMvcTest(CommentController.class)
@AutoConfigureMockMvc(addFilters = false)
public class CommentControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockitoBean
  private CommentService commentService;

  private static final UUID ARTICLE_ID = UUID.randomUUID();
  private static final UUID USER_ID = UUID.randomUUID();
  private static final UUID COMMENT_ID = UUID.randomUUID();
  private static final LocalDateTime NOW = LocalDateTime.of(2026, 1, 1, 12, 0, 0);

  private CommentDto buildCommentDto() {
    return new CommentDto(COMMENT_ID, ARTICLE_ID, USER_ID, "testUser", "test", 0L, false, NOW);
  }

  @Nested
  @DisplayName("댓글 등록 - POST")
  class CreateComment {

    @Test
    @DisplayName("정상 요청 시 201과 CommentDto를 반환")
    void createComment_success() throws Exception {
      CommentRegisterRequest request = new CommentRegisterRequest(
          ARTICLE_ID, USER_ID, "test"
      );
      CommentDto response = buildCommentDto();
      given(commentService.createComment(any(CommentRegisterRequest.class)))
          .willReturn(response);

      mockMvc.perform(post("/api/comments")
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(request)))
          .andDo(print())
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.id").value(COMMENT_ID.toString()))
          .andExpect(jsonPath("$.articleId").value(ARTICLE_ID.toString()))
          .andExpect(jsonPath("$.userId").value(USER_ID.toString()))
          .andExpect(jsonPath("$.userNickname").value("testUser"))
          .andExpect(jsonPath("$.content").value("test"))
          .andExpect(jsonPath("$.likeCount").value(0))
          .andExpect(jsonPath("$.likedByMe").value(false));
    }

    @Test
    @DisplayName("articleId가 null이면 400을 반환함")
    void createComment_nullArticleId_returns400() throws Exception {
      CommentRegisterRequest request = new CommentRegisterRequest(null, USER_ID, "test");

      mockMvc.perform(post("/api/comments")
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(request)))
          .andDo(print())
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("userId가 null이면 400을 반환함")
    void createComment_nullUserId_returns400() throws Exception {
      CommentRegisterRequest request = new CommentRegisterRequest(ARTICLE_ID, null, "test");

      mockMvc.perform(post("/api/comments")
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(request)))
          .andDo(print())
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("content가 공백이면 400을 반환함")
    void createComment_blankContent_returns400() throws Exception {
      CommentRegisterRequest request = new CommentRegisterRequest(ARTICLE_ID, USER_ID, " ");

      mockMvc.perform(post("/api/comments")
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(request)))
          .andDo(print())
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("content가 500자를 초과하면 400을 반환함")
    void createComment_contentTooLong_returns400() throws Exception {
      String tooLong = "a".repeat(501);
      CommentRegisterRequest request = new CommentRegisterRequest(ARTICLE_ID, USER_ID, tooLong);

      mockMvc.perform(post("/api/comments")
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(request)))
          .andDo(print())
          .andExpect(status().isBadRequest());
    }
  }

  @Nested
  @DisplayName("댓글 목록 조회 - GET")
  class GetComments {

    private CursorPageResponseCommentDto buildPageResponse(boolean hasNext) {
      CommentDto dto = buildCommentDto();
      return new CursorPageResponseCommentDto(
          List.of(dto),
          hasNext ? COMMENT_ID.toString() : null,
          hasNext ? NOW : null,
          1,
          10L,
          hasNext
      );
    }

    @Test
    @DisplayName("정상적인 요청(커서 없음) 시 200과 페이지 응답을 반환함")
    void getComments_noCursor_success() throws Exception {
      CursorPageResponseCommentDto response = buildPageResponse(false);
      given(commentService.getComments(
          any(CommentSearchRequest.class),
          eq(USER_ID)
      )).willReturn(response);

      mockMvc.perform(get("/api/comments")
          .param("articleId", ARTICLE_ID.toString())
          .param("limit", "10")
          .param("orderBy", "createdAt")
          .param("direction", "DESC")
          .header("Monew-Request-User-ID", USER_ID.toString()))
          .andDo(print())
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.content").isArray())
          .andExpect(jsonPath("$.content[0].id").value(COMMENT_ID.toString()))
          .andExpect(jsonPath("$.totalElements").value(10))
          .andExpect(jsonPath("$.hasNext").value(false));
    }

    @Test
    @DisplayName("커서가 있는 경우 200과 다음 커서를 포함한 응답을 반환함")
    void getComments_withCursor_success() throws Exception {
      String cursor = "2026-06-05T13:49:35.781650_2026-06-05T13:49:35.781650_" + COMMENT_ID;
      CursorPageResponseCommentDto response = buildPageResponse(true);
      given(commentService.getComments(
          any(CommentSearchRequest.class),
          eq(USER_ID)
      )).willReturn(response);

      mockMvc.perform(get("/api/comments")
          .param("articleId", ARTICLE_ID.toString())
          .param("cursor", cursor)
          .param("limit", "5")
          .param("orderBy", "createdAt")
          .param("direction", "DESC")
          .header("Monew-Request-User-ID", USER_ID.toString()))
          .andDo(print())
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.hasNext").value(true))
          .andExpect(jsonPath("$.nextCursor").value(COMMENT_ID.toString()));
    }

    @Test
    @DisplayName("likeCount 기준 정렬 시 200을 반환함")
    void getComments_orderByLikeCount_success() throws Exception {
      CursorPageResponseCommentDto response = buildPageResponse(false);
      given(commentService.getComments(
          any(CommentSearchRequest.class),
          eq(USER_ID)
      )).willReturn(response);

      mockMvc.perform(get("/api/comments")
          .param("articleId", ARTICLE_ID.toString())
          .param("limit", "10")
          .param("orderBy", "likeCount")
          .param("direction", "DESC")
          .header("Monew-Request-User-ID", USER_ID.toString()))
          .andDo(print())
          .andExpect(status().isOk());
    }

    @Test
    @DisplayName("limit이 0 이하면 400을 반환")
    void getComments_invalidLimit_returns400() throws Exception {
      mockMvc.perform(get("/api/comments")
          .param("articleId", ARTICLE_ID.toString())
          .param("limit", "0")
          .param("orderBy", "createdAt")
          .param("direction", "DESC")
          .header("Monew-Request-User-ID", USER_ID.toString()))
          .andDo(print())
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Monew-Request-User-ID 헤더가 없으면 400을 반환함")
    void getComments_missingHeader_returns400() throws Exception {
      mockMvc.perform(get("/api/comments")
          .param("articleId", ARTICLE_ID.toString())
          .param("limit", "10")
          .param("orderBy", "createdAt")
          .param("direction", "DESC"))
          .andDo(print())
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("articleId 파라미터가 없는 경우 400을 반환함")
    void getComments_missingArticleId_returns400() throws Exception {
      mockMvc.perform(get("/api/comments")
          .param("limit", "10")
          .param("orderBy", "createdAt")
          .param("direction", "DESC")
          .header("Monew-Request-User-ID", USER_ID.toString()))
          .andDo(print())
          .andExpect(status().isBadRequest());
    }
  }

  @Nested
  @DisplayName("댓글 수정 - PATCH")
  class UpdateComment {

    @Test
    @DisplayName("정상 요청 시 200과 수정된 CommentDto를 반환함")
    void updateComment_success() throws Exception {
      CommentUpdateRequest request = new CommentUpdateRequest("updateComment");
      CommentDto response = new CommentDto(
          COMMENT_ID, ARTICLE_ID, USER_ID, "testUser",
          "updateComment", 3L, true, NOW
      );
      given(commentService.updateComment(eq(COMMENT_ID), eq(USER_ID), any(CommentUpdateRequest.class)))
          .willReturn(response);

      mockMvc.perform(patch("/api/comments/{commentId}", COMMENT_ID)
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(request))
          .header("Monew-Request-User-ID", USER_ID.toString()))
          .andDo(print())
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.id").value(COMMENT_ID.toString()))
          .andExpect(jsonPath("$.content").value("updateComment"))
          .andExpect(jsonPath("$.likeCount").value(3))
          .andExpect(jsonPath("$.likedByMe").value(true));
    }

    @Test
    @DisplayName("content가 공백이면 400을 반환함")
    void updateComment_blankContent_returns400() throws Exception {
      CommentUpdateRequest request = new CommentUpdateRequest(" ");

      mockMvc.perform(patch("/api/comments/{commentId}", COMMENT_ID)
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(request))
          .header("Monew-Request-User-ID", USER_ID.toString()))
          .andDo(print())
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("content가 500자를 초과하면 400을 반환함")
    void updateComment_contentTooLong_returns400() throws Exception {
      CommentUpdateRequest request = new CommentUpdateRequest("a".repeat(501));

      mockMvc.perform(patch("/api/comments/{commentId}", COMMENT_ID)
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(request))
          .header("Monew-Request-User-ID", USER_ID.toString()))
          .andDo(print())
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Monew-Request-User-ID 헤더가 없으면 400을 반환함")
    void updateComment_missingHeader_returns400() throws Exception {
      CommentUpdateRequest request = new CommentUpdateRequest("updateComment");

      mockMvc.perform(patch("/api/comments/{commentId}", COMMENT_ID)
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(request)))
          .andDo(print())
          .andExpect(status().isBadRequest());
    }
  }

  @Nested
  @DisplayName("댓글 논리 삭제 - Soft DELETE")
  class DeleteComment {

    @Test
    @DisplayName("정상 요청 시 204를 반환하고 서비스를 호출함")
    void deleteComment_success() throws Exception {
      willDoNothing().given(commentService).deleteComment(COMMENT_ID);

      mockMvc.perform(delete("/api/comments/{commentId}", COMMENT_ID)
          .header("Monew-Request-User-ID", USER_ID.toString()))
          .andDo(print())
          .andExpect(status().isNoContent());

      verify(commentService).deleteComment(COMMENT_ID);
    }
  }

  @Nested
  @DisplayName("댓글 물릴 삭제 - Hard DELETE")
  class HardDeleteComment {

    @Test
    @DisplayName("정상 요청 시 204를 반환하고 서비스를 호출함")
    void hardDeleteComment_success() throws Exception {
      willDoNothing().given(commentService).hardDeleteComment(COMMENT_ID);

      mockMvc.perform(delete("/api/comments/{commentId}/hard", COMMENT_ID))
          .andDo(print())
          .andExpect(status().isNoContent());

      verify(commentService).hardDeleteComment(COMMENT_ID);
    }
  }
}
