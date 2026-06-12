package com.codeit.monew.infra.externalapi.hankyung.dto;

public record HankyungRssItem(
        String title,
        String link,
        String description,
        String pubDate
) {
}