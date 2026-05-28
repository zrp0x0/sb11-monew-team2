package com.codeit.monew.domain.article.dto.request;

import com.codeit.monew.domain.article.entity.ArticleSource;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Schema(description = "뉴스 기사 목록 조회 조건")
public record ArticleSearchRequest(
        @Schema(description = "검색어(제목, 요약)")
        String keyword,

        @Schema(description = "관심사 ID")
        UUID interestId,

        @Schema(description = "출처(포함)")
        List<ArticleSource> sourceIn,

        @Schema(description = "날짜 시작(범위)")
        LocalDateTime publishDateFrom,

        @Schema(description = "날짜 끝(범위)")
        LocalDateTime publishDateTo,

        @Schema(description = "정렬 속성 이름")
        String orderBy,

        @Schema(description = "정렬 방향")
        String direction,

        @Schema(description = "커서 값")
        String cursor,

        @Schema(description = "보조 커서(createdAt) 값")
        LocalDateTime after,

        @Schema(description = "커서 페이지 크기")
        int limit,

        @Schema(description = "요청자 ID")
        UUID requestUserId
) {
}