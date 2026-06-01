package com.codeit.monew.external.naver.dto;

import java.util.List;

public record NaverNewsResponse(
        String lastBuildDate,
        int total,
        int start,
        int display,
        List<NaverNewsItem> items
) {
}