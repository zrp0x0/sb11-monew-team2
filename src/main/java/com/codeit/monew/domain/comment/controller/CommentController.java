package com.codeit.monew.domain.comment.controller;

import com.codeit.monew.domain.comment.dto.CommentDto;
import com.codeit.monew.domain.comment.dto.CommentOrderBy;
import com.codeit.monew.domain.comment.dto.CommentRegisterRequest;
import com.codeit.monew.domain.comment.dto.CursorPageResponseCommentDto;
import com.codeit.monew.domain.comment.dto.SortDirection;
import com.codeit.monew.domain.comment.service.CommentService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
public class CommentController {

  private final CommentService commentService;

  @Operation(summary = "댓글 등록", description = "댓글을 등록합니다.")
  @ResponseStatus(HttpStatus.CREATED)
  @PostMapping
  public CommentDto createComment(
      @Valid @RequestBody CommentRegisterRequest request
  ) {
    return commentService.createComment(request);
  }

  @Operation(summary = "댓글 목록 조회", description = "댓글 목록을 조회합니다.")
  @ResponseStatus(HttpStatus.OK)
  @GetMapping
  public CursorPageResponseCommentDto getComments(
      @RequestParam UUID articleId,
      @RequestParam(required = false) String cursor,
      @RequestParam(required = false)LocalDateTime after,
      @RequestParam @Min(1) int limit,
      @RequestParam CommentOrderBy orderBy,
      @RequestParam SortDirection direction,
      @RequestHeader("Monew-Request-User-ID") UUID requestUserId
  ) {
    return commentService.getComments(articleId, cursor, after, limit, orderBy, direction, requestUserId);
  }
}
