package com.codeit.monew.domain.article.dto.request;

import com.codeit.monew.domain.article.entity.ArticleSource;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;

@Schema(description = "뉴스 기사 목록 조회 조건")
public record ArticleSearchRequest(
    @Schema(description = "검색어(제목, 요약)", example = "스포츠")
    String keyword,

    @Schema(description = "관심사 ID")
    UUID interestId,

    @Schema(description = "출처(포함)")
    List<ArticleSource> sourceIn,

    @Schema(description = "날짜 시작(범위)")
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSS")
    LocalDateTime publishDateFrom,

    @Schema(description = "날짜 끝(범위)")
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSS")
    LocalDateTime publishDateTo,

    @NotBlank(message = "정렬 기준은 필수입니다.")
    @Pattern(regexp = "^(publishDate|commentCount|viewCount)$", message = "정렬 기준은 publishDate, commentCount, viewCount 중 하나여야 합니다.")
    @Schema(description = "정렬 속성 이름", requiredMode = Schema.RequiredMode.REQUIRED, example = "publishDate")
    String orderBy,

    @NotBlank(message = "정렬 방향은 필수입니다.")
    @Pattern(regexp = "^(ASC|DESC)$", message = "정렬 방향은 ASC 또는 DESC여야 합니다.")
    @Schema(description = "정렬 방향", requiredMode = Schema.RequiredMode.REQUIRED, example = "DESC")
    String direction,

    @Schema(description = "커서 값")
    String cursor,

    @Schema(description = "보조 커서(createdAt) 값")
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSS")
    LocalDateTime after,

    @NotNull(message = "페이지 크기는 필수입니다.")
    @Positive(message = "페이지 크기는 1 이상이어야 합니다.")
    @Max(value = 100, message = "페이지 크기는 최대 100까지 가능합니다.")
    @Schema(description = "커서 페이지 크기", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    Integer limit,

    @Schema(description = "요청자 ID", hidden = true)
    UUID requestUserId
) {

}