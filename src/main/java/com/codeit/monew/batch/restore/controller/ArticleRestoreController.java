package com.codeit.monew.batch.restore.controller;

import com.codeit.monew.batch.restore.dto.ArticleRestoreResultResponse;
import com.codeit.monew.batch.restore.service.ArticleRestoreService;
import io.swagger.v3.oas.annotations.Operation;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/articles")
@RequiredArgsConstructor
public class ArticleRestoreController {

  private final ArticleRestoreService articleRestoreService;

  @Operation(summary = "뉴스 복구", description = "유실된 뉴스 기사를 복구.", operationId = "restore")
  @PostMapping("/restore")
  public List<ArticleRestoreResultResponse> restoreArticles(
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
  ) {
    return articleRestoreService.restoreRange(from, to);
  }
}
