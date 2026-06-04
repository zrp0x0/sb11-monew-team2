package com.codeit.monew.infra.externalapi.naver.dto;

public record NaverNewsItem(
    String title,
    String originallink, // 원본 기사 링크
    String link,         // 네이버 뉴스 링크
    String description,
    String pubDate       // 발행일 (String 타입으로 받고 나중에 파싱)
) {

}
