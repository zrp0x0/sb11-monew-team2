package com.codeit.monew.domain.article.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.codeit.monew.domain.article.dto.request.ArticleSearchRequest;
import com.codeit.monew.domain.article.dto.response.ArticleDto;
import com.codeit.monew.domain.article.entity.ArticleSource;
import com.codeit.monew.domain.article.exception.ArticleErrorCode;
import com.codeit.monew.domain.article.exception.ArticleException;
import com.codeit.monew.domain.article.service.ArticleService;
import com.codeit.monew.domain.articleView.dto.response.ArticleViewDto;
import com.codeit.monew.domain.user.exception.UserErrorCode;
import com.codeit.monew.domain.user.exception.UserException;
import com.codeit.monew.global.dto.CursorPageResponse;
import com.codeit.monew.global.error.GlobalExceptionHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
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
        mockMvc = MockMvcBuilders.standaloneSetup(articleController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper()))
                .build();
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
    @DisplayName("기사 뷰 등록에 성공하면 200 OK와 ArticleViewDto를 반환")
    void registerArticleView_success() throws Exception {
        // given
        UUID articleId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID requestUserId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        UUID articleViewId = UUID.fromString("33333333-3333-3333-3333-333333333333");
        LocalDateTime viewedAt = LocalDateTime.of(2026, 5, 28, 12, 0);
        LocalDateTime publishedAt = LocalDateTime.of(2026, 5, 27, 10, 0);
        ArticleViewDto response = new ArticleViewDto(
                articleViewId,
                requestUserId,
                viewedAt,
                articleId,
                ArticleSource.NAVER,
                "https://news.example.com/article",
                "기사 제목",
                publishedAt,
                "기사 요약",
                3L,
                10L
        );

        when(articleService.registerArticleView(eq(articleId), eq(requestUserId.toString())))
                .thenReturn(response);

        // when & then
        mockMvc.perform(post("/api/articles/{articleId}/article-views", articleId)
                        .header("Monew-Request-User-ID", requestUserId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(articleViewId.toString()))
                .andExpect(jsonPath("$.viewedBy").value(requestUserId.toString()))
                .andExpect(jsonPath("$.createdAt").value("2026-05-28T12:00:00"))
                .andExpect(jsonPath("$.articleId").value(articleId.toString()))
                .andExpect(jsonPath("$.source").value("NAVER"))
                .andExpect(jsonPath("$.sourceUrl").value("https://news.example.com/article"))
                .andExpect(jsonPath("$.articleTitle").value("기사 제목"))
                .andExpect(jsonPath("$.articlePublishedDate").value("2026-05-27T10:00:00"))
                .andExpect(jsonPath("$.articleSummary").value("기사 요약"))
                .andExpect(jsonPath("$.articleCommentCount").value(3))
                .andExpect(jsonPath("$.articleViewCount").value(10));

        verify(articleService).registerArticleView(articleId, requestUserId.toString());
    }

    @Test
    @DisplayName("요청자 헤더가 없으면 기사 뷰 등록은 401 Unauthorized를 반환")
    void registerArticleView_fail_whenRequestUserHeaderMissing() throws Exception {
        // given
        UUID articleId = UUID.fromString("11111111-1111-1111-1111-111111111111");

        when(articleService.registerArticleView(eq(articleId), isNull()))
                .thenThrow(new UserException(UserErrorCode.REQUEST_USER_ID_REQUIRED));

        // when & then
        mockMvc.perform(post("/api/articles/{articleId}/article-views", articleId))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("REQUEST_USER_ID_REQUIRED"));
    }

    @Test
    @DisplayName("요청자 헤더가 UUID 형식이 아니면 기사 뷰 등록은 401 Unauthorized를 반환")
    void registerArticleView_fail_whenRequestUserHeaderInvalid() throws Exception {
        // given
        UUID articleId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        String requestUserId = "invalid-user-id";

        when(articleService.registerArticleView(eq(articleId), eq(requestUserId)))
                .thenThrow(new UserException(UserErrorCode.REQUEST_USER_ID_REQUIRED));

        // when & then
        mockMvc.perform(post("/api/articles/{articleId}/article-views", articleId)
                        .header("Monew-Request-User-ID", requestUserId))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("REQUEST_USER_ID_REQUIRED"));
    }

    @Test
    @DisplayName("존재하지 않는 사용자의 기사 뷰 등록은 401 Unauthorized를 반환")
    void registerArticleView_fail_whenUserNotFound() throws Exception {
        // given
        UUID articleId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID requestUserId = UUID.fromString("22222222-2222-2222-2222-222222222222");

        when(articleService.registerArticleView(eq(articleId), eq(requestUserId.toString())))
                .thenThrow(new UserException(UserErrorCode.INVALID_CREDENTIALS));

        // when & then
        mockMvc.perform(post("/api/articles/{articleId}/article-views", articleId)
                        .header("Monew-Request-User-ID", requestUserId.toString()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    @DisplayName("존재하지 않는 기사의 기사 뷰 등록은 404 Not Found를 반환")
    void registerArticleView_fail_whenArticleNotFound() throws Exception {
        // given
        UUID articleId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID requestUserId = UUID.fromString("22222222-2222-2222-2222-222222222222");

        when(articleService.registerArticleView(eq(articleId), eq(requestUserId.toString())))
                .thenThrow(new ArticleException(ArticleErrorCode.ARTICLE_NOT_FOUND));

        // when & then
        mockMvc.perform(post("/api/articles/{articleId}/article-views", articleId)
                        .header("Monew-Request-User-ID", requestUserId.toString()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value("ARTICLE_NOT_FOUND"));
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

    private ObjectMapper objectMapper() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }
}
