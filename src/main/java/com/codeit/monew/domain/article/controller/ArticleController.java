package com.codeit.monew.domain.article.controller;

import com.codeit.monew.domain.article.dto.request.ArticleSearchRequest;
import com.codeit.monew.domain.article.dto.request.CursorPageResponseDate;
import com.codeit.monew.domain.article.dto.response.ArticleDto;
import com.codeit.monew.domain.article.entity.ArticleSource;
import com.codeit.monew.domain.article.service.ArticleService;
import com.codeit.monew.domain.articleView.dto.response.ArticleViewDto;
import io.swagger.v3.oas.annotations.Operation;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/articles")
public class ArticleController {

    private final ArticleService articleService;

    @Operation(summary = "출처 목록 조회", description = "서비스에서 지원하는 출처 목록을 조회합니다.")
    @GetMapping("/sources")
    public List<String> getSources() {
        log.info("뉴스 기사 출처 목록 조회 요청");
        return articleService.getSources();
    }

    @Operation(summary = "뉴스 기사 목록 조회", description = "조건에 맞는 뉴스 기사 목록을 조회합니다.")
    @GetMapping
    public CursorPageResponseDate<ArticleDto> searchArticles(
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) UUID interestId,
        @RequestParam(required = false) List<ArticleSource> sourceIn,
        @RequestParam(required = false)
        @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSS") // 패턴 일치
        LocalDateTime publishDateFrom,
        @RequestParam(required = false)
        @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSS") // 패턴 일치
        LocalDateTime publishDateTo,
        @RequestParam(required = false, defaultValue = "publishDate") String orderBy,
        // 필수 해제 및 기본값 지정
        @RequestParam(required = false, defaultValue = "DESC") String direction,
        // 필수 해제 및 기본값 지정
        @RequestParam(required = false) String cursor,
        @RequestParam(required = false)
        @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSS") // 보조 커서 정밀도 강제 고정
        LocalDateTime after,
        @RequestParam(defaultValue = "10") int limit, // 기본값 설정해 두면 프론트가 편해요!
        @RequestHeader("Monew-Request-User-ID") UUID requestUserId
    ) {
        log.info("=====================");
        log.info("cursor: {}", cursor);
        if (after != null) {
            log.info("after: {}", after.toString());
        } else {
            log.info("after null");
        }

        ArticleSearchRequest request = new ArticleSearchRequest(
            keyword,
            interestId,
            sourceIn,
            publishDateFrom,
            publishDateTo,
            orderBy,
            direction,
            cursor,
            after,
            limit,
            requestUserId
        );

        log.info("뉴스 기사 목록 조회 요청. requestUserId: {}, orderBy: {}, direction: {}, limit: {}",
            requestUserId, orderBy, direction, limit);

        return articleService.searchArticles(request);
    }

    @Operation(summary = "기사 뷰 등록", description = "기사 뷰를 등록합니다.")
    @PostMapping("/{articleId}/article-views")
    public ArticleViewDto registerArticleView(
        @PathVariable UUID articleId,
        @RequestHeader(value = "Monew-Request-User-ID", required = false) String requestUserId
    ) {
        log.info("기사 뷰 등록 요청. articleId: {}, requestUserId: {}", articleId, requestUserId);
        return articleService.registerArticleView(articleId, requestUserId);
    }

    @Operation(summary = "뉴스 기사 단건 조회", description = "뉴스 기사 ID로 뉴스 기사 단건을 조회합니다.")
    @GetMapping("/{articleId}")
    public ArticleDto getArticle(
        @PathVariable UUID articleId,
        @RequestHeader(value = "Monew-Request-User-ID", required = false) String requestUserId
    ) {
        log.info("뉴스 기사 단건 조회 요청. articleId: {}, requestUserId: {}", articleId, requestUserId);
        return articleService.getArticle(articleId, requestUserId);
    }
}
