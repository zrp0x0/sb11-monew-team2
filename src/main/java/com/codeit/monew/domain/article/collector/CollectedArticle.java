package com.codeit.monew.domain.article.collector;

import com.codeit.monew.domain.article.entity.Article;
import com.codeit.monew.domain.article.entity.ArticleSource;
import java.time.LocalDateTime;

public record CollectedArticle(
        ArticleSource source,
        String sourceUrl,
        String title,
        String summary,
        LocalDateTime publishedAt
) {

    public Article toArticle() {
        return Article.create(
                source,
                sourceUrl,
                title,
                summary,
                publishedAt
        );
    }
}