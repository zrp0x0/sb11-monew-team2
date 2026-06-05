package com.codeit.monew.infra.externalapi.naver.client;

import com.codeit.monew.infra.externalapi.naver.dto.NaverNewsResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "naverNewsClient", url = "${external-api.naver.url}")
public interface NaverNewsClient {

    @GetMapping("/v1/search/news.json")
    NaverNewsResponse searchNews(
        @RequestHeader("X-Naver-Client-Id") String clientId,
        @RequestHeader("X-Naver-Client-Secret") String clientSecret,
        @RequestParam("query") String query,      // 검색어 (관심사 키워드)
        @RequestParam("display") int display,     // 가져올 개수 (최대 100)
        @RequestParam("start") int start,         // 시작 위치
        @RequestParam("sort") String sort         // 정렬 (sim: 유사도, date: 날짜순)
    );
}
