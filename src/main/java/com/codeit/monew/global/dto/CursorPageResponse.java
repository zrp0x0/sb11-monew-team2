package com.codeit.monew.global.dto;

import java.util.List;

public record CursorPageResponse<T>(
        List<T> content,
        String nextCursor,
        String nextAfter,
        int size,
        Long totalElements,
        Boolean hasNext
) {

}
