package com.codeit.monew.external.naver;

import com.codeit.monew.external.naver.dto.NaverNewsResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(
        name = "naver-news-client",
        url = "https://openapi.naver.com"
)
public interface NaverNewsClient {

    @GetMapping("/v1/search/news.json")
    NaverNewsResponse searchNews(
            @RequestHeader("X-Naver-Client-Id") String clientId,
            @RequestHeader("X-Naver-Client-Secret") String clientSecret,
            @RequestParam String query,
            @RequestParam(defaultValue = "100") int display,
            @RequestParam(defaultValue = "1") int start,
            @RequestParam(defaultValue = "date") String sort
    );
}