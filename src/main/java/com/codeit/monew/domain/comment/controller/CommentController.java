package com.codeit.monew.domain.comment.controller;

import com.codeit.monew.domain.comment.dto.CommentDto;
import com.codeit.monew.domain.comment.dto.CommentRegisterRequest;
import com.codeit.monew.domain.comment.service.CommentService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
}
