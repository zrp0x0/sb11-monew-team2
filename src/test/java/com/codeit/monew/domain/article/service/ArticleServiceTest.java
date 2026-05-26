package com.codeit.monew.domain.article.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ArticleServiceTest {

    private final ArticleService articleService = new ArticleService();

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