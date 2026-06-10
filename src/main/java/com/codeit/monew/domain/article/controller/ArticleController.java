package com.codeit.monew.domain.article.controller;

import static com.codeit.monew.global.filter.MDCLoggingFilter.HEADER_USER_ID;

import com.codeit.monew.domain.article.dto.request.ArticleSearchRequest;
import com.codeit.monew.domain.article.dto.response.ArticleDto;
import com.codeit.monew.domain.article.service.ArticleService;
import com.codeit.monew.domain.articleView.dto.response.ArticleViewDto;
import com.codeit.monew.global.dto.CursorPageResponse;
import com.codeit.monew.domain.article.entity.ArticleSource;
import org.springframework.web.bind.annotation.RequestParam;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/articles")
public class ArticleController {

  private final ArticleService articleService;
  private final ArticleSearchRequestNormalizer articleSearchRequestNormalizer;

  @Operation(summary = "출처 목록 조회", description = "서비스에서 지원하는 출처 목록을 조회합니다.")
  @GetMapping("/sources")
  @ResponseStatus(HttpStatus.OK)
  public List<String> getSources() {
    log.info("뉴스 기사 출처 목록 조회 요청");
    return articleService.getSources();
  }

  @Operation(summary = "뉴스 기사 목록 조회", description = "조건에 맞는 뉴스 기사 목록을 조회합니다.")
  @GetMapping
  @ResponseStatus(HttpStatus.OK)
  public CursorPageResponse<ArticleDto> searchArticles(
          @Valid @ModelAttribute ArticleSearchRequest request,
          @RequestParam(value = "sourceIn[]", required = false) List<ArticleSource> sourceInBrackets,
          @RequestHeader(HEADER_USER_ID) UUID requestUserId
  ) {
    ArticleSearchRequest normalizedRequest = articleSearchRequestNormalizer.normalize(
            request,
            sourceInBrackets
    );

    return articleService.searchArticles(normalizedRequest, requestUserId);
  }

  @Operation(summary = "기사 뷰 등록", description = "기사 뷰를 등록합니다.")
  @PostMapping("/{articleId}/article-views")
  @ResponseStatus(HttpStatus.OK)
  public ArticleViewDto registerArticleView(
      @PathVariable UUID articleId,
      @RequestHeader(value = "Monew-Request-User-ID", required = false) String requestUserId
  ) {
    log.info("기사 뷰 등록 요청. articleId: {}, requestUserId: {}", articleId, requestUserId);
    return articleService.registerArticleView(articleId, requestUserId);
  }

  @Operation(summary = "뉴스 기사 단건 조회", description = "뉴스 기사 ID로 뉴스 기사 단건을 조회합니다.")
  @GetMapping("/{articleId}")
  @ResponseStatus(HttpStatus.OK)
  public ArticleDto getArticle(
      @PathVariable UUID articleId,
      @RequestHeader(value = "Monew-Request-User-ID", required = false) String requestUserId
  ) {
    log.info("뉴스 기사 단건 조회 요청. articleId: {}, requestUserId: {}", articleId, requestUserId);
    return articleService.getArticle(articleId, requestUserId);
  }
}
