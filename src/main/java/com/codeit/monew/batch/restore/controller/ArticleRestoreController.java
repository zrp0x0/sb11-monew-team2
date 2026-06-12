package com.codeit.monew.batch.restore.controller;

import com.codeit.monew.batch.restore.dto.ArticleRestoreResultResponse;
import com.codeit.monew.batch.restore.service.ArticleRestoreService;
import io.swagger.v3.oas.annotations.Operation;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/articles")
@RequiredArgsConstructor
public class ArticleRestoreController {

  private final ArticleRestoreService articleRestoreService;

  @Operation(summary = "뉴스 복구", description = "유실된 뉴스 기사를 복구.", operationId = "restore")
  @GetMapping("/restore")
  public List<ArticleRestoreResultResponse> restoreArticles(
      @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
      @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to
  ) {
    return articleRestoreService.restoreRange(from.toLocalDate(), to.toLocalDate());
  }
}