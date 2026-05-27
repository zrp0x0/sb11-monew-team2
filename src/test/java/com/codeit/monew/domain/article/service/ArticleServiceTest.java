package com.codeit.monew.domain.article.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.codeit.monew.domain.article.dto.request.ArticleSearchRequest;
import com.codeit.monew.domain.article.dto.response.ArticleDto;
import com.codeit.monew.domain.article.entity.Article;
import com.codeit.monew.domain.article.entity.ArticleSource;
import com.codeit.monew.domain.article.repository.ArticleRepository;
import com.codeit.monew.global.dto.CursorPageResponse;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ArticleServiceTest {

    @Mock
    private ArticleRepository articleRepository;

    @InjectMocks
    private ArticleService articleService;

    @Test
    @DisplayName("서비스에서 지원하는 뉴스 기사 출처 목록을 반환한다")
    void getSources_success() {
        // when
        List<String> sources = articleService.getSources();

        // then
        assertThat(sources).containsExactly(
                "NAVER",
                "HANKYUNG",
                "CHOSUN",
                "YEONHAP"
        );
    }

    @Test
    @DisplayName("뉴스 기사 목록을 조회하고 ArticleDto 페이지 응답으로 변환한다")
    void searchArticles_success() {
        // given
        UUID requestUserId = UUID.randomUUID();

        ArticleSearchRequest request = new ArticleSearchRequest(
                null,
                null,
                null,
                null,
                null,
                "publishDate",
                "DESC",
                null,
                null,
                10,
                requestUserId
        );

        Article article = Article.create(
                ArticleSource.NAVER,
                "https://news.naver.com/sample",
                "테스트 기사 제목",
                "테스트 기사 요약",
                LocalDateTime.of(2026, 5, 27, 10, 0)
        );

        CursorPageResponse<Article> articlePage = new CursorPageResponse<>(
                List.of(article),
                null,
                null,
                1,
                1L,
                false
        );

        when(articleRepository.searchArticles(any(ArticleSearchRequest.class)))
                .thenReturn(articlePage);

        // when
        CursorPageResponse<ArticleDto> response = articleService.searchArticles(request);

        // then
        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0).source()).isEqualTo(ArticleSource.NAVER);
        assertThat(response.content().get(0).sourceUrl()).isEqualTo("https://news.naver.com/sample");
        assertThat(response.content().get(0).title()).isEqualTo("테스트 기사 제목");
        assertThat(response.content().get(0).summary()).isEqualTo("테스트 기사 요약");
        assertThat(response.content().get(0).commentCount()).isEqualTo(0L);
        assertThat(response.content().get(0).viewCount()).isEqualTo(0L);
        assertThat(response.content().get(0).viewedByMe()).isFalse();

        assertThat(response.size()).isEqualTo(1);
        assertThat(response.totalElements()).isEqualTo(1L);
        assertThat(response.hasNext()).isFalse();

        verify(articleRepository).searchArticles(request);
    }
}