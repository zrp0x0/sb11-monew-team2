package com.codeit.monew.domain.articleView.dto.response;

import com.codeit.monew.domain.article.entity.Article;
import com.codeit.monew.domain.article.entity.ArticleSource;
import com.codeit.monew.domain.articleView.entity.ArticleView;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "기사 조회 정보")
public record ArticleViewDto(
        @Schema(description = "기사 조회 ID")
        UUID id,

        @Schema(description = "기사를 조회한 사용자 ID")
        UUID viewedBy,

        @Schema(description = "기사를 본 날짜")
        LocalDateTime createdAt,

        @Schema(description = "기사 ID")
        UUID articleId,

        @Schema(description = "출처")
        ArticleSource source,

        @Schema(description = "원본 기사 URL")
        String sourceUrl,

        @Schema(description = "제목")
        String articleTitle,

        @Schema(description = "날짜")
        LocalDateTime articlePublishedDate,

        @Schema(description = "요약")
        String articleSummary,

        @Schema(description = "댓글 수")
        long articleCommentCount,

        @Schema(description = "조회 수")
        long articleViewCount
) {

    public static ArticleViewDto from(ArticleView articleView) {
        Article article = articleView.getArticle();
        return new ArticleViewDto(
                articleView.getId(),
                articleView.getUser().getId(),
                articleView.getCreatedAt(),
                article.getId(),
                article.getSource(),
                article.getSourceUrl(),
                article.getTitle(),
                article.getPublishedAt(),
                article.getSummary(),
                article.getCommentCount(),
                article.getViewCount()
        );
    }
}
