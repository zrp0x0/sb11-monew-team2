package com.codeit.monew.domain.commentLike.controller;

import com.codeit.monew.domain.commentLike.dto.CommentLikeDto;
import com.codeit.monew.domain.commentLike.service.CommentLikeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
public class CommentLikeController implements CommentLikeApi {

    private final CommentLikeService commentLikeService;

    @PostMapping("/{commentId}/comment-likes")
    public ResponseEntity<CommentLikeDto> create(
            @PathVariable UUID commentId,
            @RequestHeader("Monew-Request-User-ID") UUID requestUserId
    ) {
        return ResponseEntity.ok(commentLikeService.create(commentId, requestUserId));
    }

    @DeleteMapping("/{commentId}/comment-likes")
    public ResponseEntity<Void> delete(
            @PathVariable UUID commentId,
            @RequestHeader("Monew-Request-User-ID") UUID requestUserId
    ) {
        commentLikeService.delete(commentId, requestUserId);
        return ResponseEntity.ok().build();
    }

}
