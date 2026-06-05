package com.codeit.monew.domain.article.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;
import java.util.List;

public record CursorPageResponseDate<T>(
    List<T> content,
    String nextCursor,
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSS") // 형식을 확실하게 고정
    LocalDateTime nextAfter,
    int size,
    Long totalElements,
    Boolean hasNext
) {

}
