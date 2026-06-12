package com.codeit.monew.domain.commentLike.controller;

import com.codeit.monew.domain.commentLike.dto.CommentLikeDto;
import com.codeit.monew.domain.commentLike.service.CommentLikeService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
public class CommentLikeController {

    private final CommentLikeService commentLikeService;

    @Operation(summary = "관심사 댓글 좋아요", description = "댓글 좋아요를 등록합니다.")
    @PostMapping("/{commentId}/comment-likes")
    @ResponseStatus(HttpStatus.OK)
    public CommentLikeDto create(
            @PathVariable UUID commentId,
            @RequestHeader("Monew-Request-User-ID") UUID requestUserId
    ) {
        return commentLikeService.create(commentId, requestUserId);
    }

    @Operation(summary = "댓글 좋아요 취소", description = "댓글 좋아요를 취소합니다.")
    @DeleteMapping("/{commentId}/comment-likes")
    @ResponseStatus(HttpStatus.OK)
    public void delete(
            @PathVariable UUID commentId,
            @RequestHeader("Monew-Request-User-ID") UUID requestUserId
    ) {
        commentLikeService.delete(commentId, requestUserId);
    }

}
