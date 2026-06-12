package com.codeit.monew.domain.interest.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDateTime;

public record InterestSearchRequest(
    String keyword,

    @Pattern(regexp = "^(name|subscriberCount)$", message = "정렬 기준은 'name'또는 'subscriberCount'만 가능합니다.")
    String orderBy,

    @Pattern(regexp = "^(?i)(ASC|DESC)$", message = "정렬 방향은 'ASC'또는 'DESC'만 가능합니다.")
    String direction,

    String cursor,

    LocalDateTime after,

    @Min(value = 1, message = "페이지 크기는 최소 1 이상이어야 합니다.")
    @Max(value = 100, message = "페이지 크기는 최대 100을 초과할 수 없습니다.")
    Integer limit
) {

  public String getOrderBy() {
    return (orderBy == null || orderBy.isBlank()) ? "name" : orderBy;
  }

  public String getDirection() {
    return (direction == null || direction.isBlank()) ? "DESC" : direction;
  }

  public Integer getLimit() {
    return (limit == null) ? 6 : limit;
  }
}
