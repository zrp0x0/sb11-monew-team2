package com.codeit.monew.domain.comment.controller;

import com.codeit.monew.domain.comment.dto.CommentDto;
import com.codeit.monew.domain.comment.dto.CommentRegisterRequest;
import com.codeit.monew.domain.comment.dto.CommentSearchRequest;
import com.codeit.monew.domain.comment.dto.CommentUpdateRequest;
import com.codeit.monew.domain.comment.service.CommentService;
import com.codeit.monew.global.dto.CursorPageResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
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
  public CursorPageResponse<CommentDto> getComments(
      @ModelAttribute @Valid CommentSearchRequest request,
      @RequestHeader("Monew-Request-User-ID") UUID requestUserId
  ) {
    return commentService.getComments(request, requestUserId);
  }

  @Operation(summary = "댓글 정보 수정", description = "댓글의 내용을 수정합니다.")
  @ResponseStatus(HttpStatus.OK)
  @PatchMapping("/{commentId}")
  public CommentDto updateComment(
      @PathVariable UUID commentId,
      @RequestHeader("Monew-Request-User-ID") UUID requestUserId,
      @Valid @RequestBody CommentUpdateRequest request
  ) {
    return commentService.updateComment(commentId, requestUserId, request);
  }

  @Operation(summary = "댓글 논리 삭제", description = "댓글을 논리적으로 삭제합니다.")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @DeleteMapping("/{commentId}")
  public void deleteComment(
      @PathVariable UUID commentId
  ) {
    commentService.deleteComment(commentId);
  }

  @Operation(summary = "댓글 물리 삭제", description = "댓글을 물리적으로 삭제합니다.")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @DeleteMapping("/{commentId}/hard")
  public void hardDeleteComment(
      @PathVariable UUID commentId
  ) {
    commentService.hardDeleteComment(commentId);
  }
}
