package com.codeit.monew.domain.article.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.codeit.monew.domain.article.repository.ArticleRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class ArticleServiceTest {

    private ArticleRepository articleRepository;
    private ArticleService articleService;

    @BeforeEach
    void setUp() {
        articleRepository = Mockito.mock(ArticleRepository.class);
        articleService = new ArticleService(articleRepository);
    }

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
}