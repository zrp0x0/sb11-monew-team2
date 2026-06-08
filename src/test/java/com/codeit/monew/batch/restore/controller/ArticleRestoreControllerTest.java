package com.codeit.monew.batch.restore.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.codeit.monew.batch.restore.dto.ArticleRestoreResultResponse;
import com.codeit.monew.batch.restore.service.ArticleRestoreService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ArticleRestoreController.class)
@AutoConfigureMockMvc(addFilters = false)
public class ArticleRestoreControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private ArticleRestoreService articleRestoreService;

  @Test
  @DisplayName("뉴스 복구 API 호출 시 성공 응답을 반환한다.")
  void restoreArticles_Success() throws Exception {
    //given
    LocalDate from = LocalDate.of(2026, 1, 1);
    LocalDate to = LocalDate.of(2026, 1, 5);

    ArticleRestoreResultResponse mockResponse = ArticleRestoreResultResponse.of(
        LocalDateTime.now(),
        List.of("기사1", "기사2")
    );

    when(articleRestoreService.restoreRange(any(LocalDate.class), any(LocalDate.class)))
        .thenReturn(List.of(mockResponse));

    //when & then
    mockMvc.perform(post("/api/articles/restore")
            .param("from", from.toString())
            .param("to", to.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].restoredArticleCount").value(2))
        .andExpect(jsonPath("$[0].restoredArticleIds[0]").value("기사1"))
        .andExpect(jsonPath("$[0].restoredArticleIds[1]").value("기사2"));
  }
}
