package com.codeit.monew.domain.userActivity.dto;

import com.codeit.monew.domain.article.entity.Article;
import com.codeit.monew.domain.article.entity.ArticleSource;
import com.codeit.monew.domain.articleView.entity.ArticleView;
import java.time.LocalDateTime;
import java.util.UUID;

public record UserActivityArticleViewDto(
        UUID id,
        UUID articleId,
        ArticleSource source,
        String sourceUrl,
        String articleTitle,
        LocalDateTime articlePublishedDate,
        String articleSummary,
        long articleCommentCount,
        long articleViewCount,
        LocalDateTime createdAt
) {

    public static UserActivityArticleViewDto from(ArticleView articleView) {
        Article article = articleView.getArticle();
        return new UserActivityArticleViewDto(
                articleView.getId(),
                article.getId(),
                article.getSource(),
                article.getSourceUrl(),
                article.getTitle(),
                article.getPublishedAt(),
                article.getSummary(),
                article.getCommentCount(),
                article.getViewCount(),
                articleView.getCreatedAt()
        );
    }
}
