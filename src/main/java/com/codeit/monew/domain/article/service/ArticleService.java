package com.codeit.monew.domain.article.service;

import com.codeit.monew.domain.article.dto.request.ArticleSearchRequest;
import com.codeit.monew.domain.article.dto.response.ArticleDto;
import com.codeit.monew.domain.article.entity.Article;
import com.codeit.monew.domain.article.entity.ArticleSource;
import com.codeit.monew.domain.article.repository.ArticleRepository;
import com.codeit.monew.global.dto.CursorPageResponse;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ArticleService {

    private final ArticleRepository articleRepository;

    public List<String> getSources() {
        return Arrays.stream(ArticleSource.values())
                .map(Enum::name)
                .toList();
    }

    @Transactional(readOnly = true)
    public CursorPageResponse<ArticleDto> searchArticles(ArticleSearchRequest request) {
        CursorPageResponse<Article> articlePage = articleRepository.searchArticles(request);

        List<ArticleDto> content = articlePage.content()
                .stream()
                .map(article -> ArticleDto.from(article, false))
                .toList();

        return new CursorPageResponse<>(
                content,
                articlePage.nextCursor(),
                articlePage.nextAfter(),
                articlePage.size(),
                articlePage.totalElements(),
                articlePage.hasNext()
        );
    }
}