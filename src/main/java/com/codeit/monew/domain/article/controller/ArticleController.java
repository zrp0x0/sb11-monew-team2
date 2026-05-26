package com.codeit.monew.domain.article.controller;

import com.codeit.monew.domain.article.service.ArticleService;
import io.swagger.v3.oas.annotations.Operation;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/articles")
public class ArticleController {

    private final ArticleService articleService;

    @Operation(summary = "뉴스 기사 출처 목록 조회", description = "저장된 뉴스 기사들의 출처 목록을 조회합니다.")
    @GetMapping("/sources")
    public List<String> getSources() {
        log.info("뉴스 기사 출처 목록 조회 요청");
        return articleService.getSources();
    }
}