package com.codeit.monew.domain.article.dto.response;

import com.codeit.monew.domain.article.entity.Article;
import com.codeit.monew.domain.article.entity.ArticleSource;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "뉴스 기사 정보")
public record ArticleDto(
        @Schema(description = "기사 ID")
        UUID id,

        @Schema(description = "출처")
        ArticleSource source,

        @Schema(description = "원본 기사 URL")
        String sourceUrl,

        @Schema(description = "제목")
        String title,

        @Schema(description = "날짜")
        LocalDateTime publishDate,

        @Schema(description = "요약")
        String summary,

        @Schema(description = "댓글 수")
        long commentCount,

        @Schema(description = "조회 수")
        long viewCount,

        @Schema(description = "요청자의 조회 여부")
        boolean viewedByMe
) {

    public static ArticleDto from(Article article, boolean viewedByMe) {
        return new ArticleDto(
                article.getId(),
                article.getSource(),
                article.getSourceUrl(),
                article.getTitle(),
                article.getPublishedAt(),
                article.getSummary(),
                article.getCommentCount(),
                article.getViewCount(),
                viewedByMe
        );
    }
}