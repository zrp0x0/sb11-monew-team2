package com.codeit.monew.domain.comment.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;

public record CommentSearchRequest(
    @NotNull(message = "articleId는 필수입니다.")
    UUID articleId,

    String cursor,

    @DateTimeFormat(iso = ISO.DATE_TIME)
    LocalDateTime after,

    @Min(value = 1, message = "페이지 최소 크기는 1 이상이어야 합니다.")
    Integer limit,

    String orderBy,

    @Pattern(regexp = "^(?i)(ASC|DESC)$", message = "정렬 방향은 'ASC'또는 'DESC'만 가능합니다.")
    String direction
) {

  public CommentOrderBy getOrderBy() {
    if(orderBy == null || orderBy.isBlank()) {
      return CommentOrderBy.CREATED_AT;
    }
    String converted = orderBy
        .replaceAll("([A-Z])", "_$1")
        .toUpperCase();
    return CommentOrderBy.valueOf(converted);
  }

  public SortDirection getDirection() {
    return (direction == null || direction.isBlank())
        ? SortDirection.DESC
        : SortDirection.valueOf(direction.toUpperCase());
  }

  public Integer getLimit() {
    return (limit == null) ? 20 : limit;
  }
}
