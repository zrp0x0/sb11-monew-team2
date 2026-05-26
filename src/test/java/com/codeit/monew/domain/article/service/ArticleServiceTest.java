package com.codeit.monew.domain.article.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.codeit.monew.domain.article.entity.ArticleSource;
import com.codeit.monew.domain.article.repository.ArticleRepository;
import java.util.List;
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
    @DisplayName("뉴스 기사 출처 목록을 문자열 목록으로 반환")
    void getSources_success() {
        // given
        when(articleRepository.findDistinctSources())
                .thenReturn(List.of(ArticleSource.NAVER, ArticleSource.HANKYUNG));

        // when
        List<String> sources = articleService.getSources();

        // then
        assertThat(sources).containsExactly("NAVER", "HANKYUNG");
    }

    @Test
    @DisplayName("저장된 뉴스 기사 출처가 없으면 빈 목록을 반환")
    void getSources_empty() {
        // given
        when(articleRepository.findDistinctSources())
                .thenReturn(List.of());

        // when
        List<String> sources = articleService.getSources();

        // then
        assertThat(sources).isEmpty();
    }
}