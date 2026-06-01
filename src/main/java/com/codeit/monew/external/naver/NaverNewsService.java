package com.codeit.monew.external.naver;

import com.codeit.monew.external.naver.config.NaverProperties;
import com.codeit.monew.external.naver.dto.NaverNewsResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NaverNewsService {

    private final NaverNewsClient naverNewsClient;
    private final NaverProperties naverProperties;

    public NaverNewsResponse searchNews(String keyword) {

        return naverNewsClient.searchNews(
                naverProperties.getClientId(),
                naverProperties.getClientSecret(),
                keyword,
                100,
                1,
                "date"
        );
    }
}