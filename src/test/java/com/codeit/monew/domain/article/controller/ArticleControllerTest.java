package com.codeit.monew.domain.article.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.codeit.monew.domain.article.dto.request.ArticleSearchRequest;
import com.codeit.monew.domain.article.dto.response.ArticleDto;
import com.codeit.monew.domain.article.entity.ArticleSource;
import com.codeit.monew.domain.article.service.ArticleService;
import com.codeit.monew.global.dto.CursorPageResponse;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class ArticleControllerTest {

    @Mock
    private ArticleService articleService;

    @InjectMocks
    private ArticleController articleController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(articleController).build();
    }

    @Test
    @DisplayName("뉴스 기사 출처 목록 조회에 성공한다")
    void getSources_success() throws Exception {
        // given
        when(articleService.getSources())
                .thenReturn(List.of("NAVER", "HANKYUNG", "CHOSUN", "YEONHAP"));

        // when & then
        mockMvc.perform(get("/api/articles/sources"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value("NAVER"))
                .andExpect(jsonPath("$[1]").value("HANKYUNG"))
                .andExpect(jsonPath("$[2]").value("CHOSUN"))
                .andExpect(jsonPath("$[3]").value("YEONHAP"));

        verify(articleService).getSources();
    }

    @Test
    @DisplayName("뉴스 기사 목록 조회에 성공한다")
    void searchArticles_success() throws Exception {
        // given
        UUID requestUserId = UUID.randomUUID();
        UUID articleId = UUID.randomUUID();

        ArticleDto articleDto = new ArticleDto(
                articleId,
                ArticleSource.NAVER,
                "https://news.naver.com/sample",
                "테스트 기사 제목",
                LocalDateTime.of(2026, 5, 27, 10, 0),
                "테스트 기사 요약",
                3L,
                10L,
                false
        );

        CursorPageResponse<ArticleDto> response = new CursorPageResponse<>(
                List.of(articleDto),
                null,
                null,
                1,
                1L,
                false
        );

        when(articleService.searchArticles(any(ArticleSearchRequest.class)))
                .thenReturn(response);

        // when & then
        mockMvc.perform(get("/api/articles")
                        .param("orderBy", "publishDate")
                        .param("direction", "DESC")
                        .param("limit", "10")
                        .header("Monew-Request-User-ID", requestUserId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(articleId.toString()))
                .andExpect(jsonPath("$.content[0].source").value("NAVER"))
                .andExpect(jsonPath("$.content[0].sourceUrl").value("https://news.naver.com/sample"))
                .andExpect(jsonPath("$.content[0].title").value("테스트 기사 제목"))
                .andExpect(jsonPath("$.content[0].summary").value("테스트 기사 요약"))
                .andExpect(jsonPath("$.content[0].commentCount").value(3))
                .andExpect(jsonPath("$.content[0].viewCount").value(10))
                .andExpect(jsonPath("$.content[0].viewedByMe").value(false))
                .andExpect(jsonPath("$.size").value(1))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.hasNext").value(false));

        ArgumentCaptor<ArticleSearchRequest> captor =
                ArgumentCaptor.forClass(ArticleSearchRequest.class);

        verify(articleService).searchArticles(captor.capture());

        ArticleSearchRequest request = captor.getValue();

        assertThat(request.orderBy()).isEqualTo("publishDate");
        assertThat(request.direction()).isEqualTo("DESC");
        assertThat(request.limit()).isEqualTo(10);
        assertThat(request.requestUserId()).isEqualTo(requestUserId);
    }

    @Test
    @DisplayName("뉴스 기사 단건 조회에 성공한다")
    void getArticle_success() throws Exception {
        // given
        UUID articleId = UUID.randomUUID();
        UUID requestUserId = UUID.randomUUID();

        ArticleDto articleDto = new ArticleDto(
                articleId,
                ArticleSource.NAVER,
                "https://news.naver.com/sample",
                "테스트 기사 제목",
                LocalDateTime.of(2026, 5, 27, 10, 30),
                "테스트 기사 요약",
                3L,
                10L,
                false
        );

        when(articleService.getArticle(articleId, requestUserId.toString()))
                .thenReturn(articleDto);

        // when & then
        mockMvc.perform(get("/api/articles/{articleId}", articleId)
                .header("Monew-Request-User-ID", requestUserId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(articleId.toString()))
                .andExpect(jsonPath("$.source").value("NAVER"))
                .andExpect(jsonPath("$.sourceUrl").value("https://news.naver.com/sample"))
                .andExpect(jsonPath("$.title").value("테스트 기사 제목"))
                .andExpect(jsonPath("$.summary").value("테스트 기사 요약"))
                .andExpect(jsonPath("$.commentCount").value(3))
                .andExpect(jsonPath("$.viewCount").value(10))
                .andExpect(jsonPath("$.viewedByMe").value(false));

        verify(articleService).getArticle(articleId, requestUserId.toString());
    }
}