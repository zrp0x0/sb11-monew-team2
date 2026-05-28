package com.codeit.monew.domain.commentLike.controller;

import com.codeit.monew.domain.commentLike.dto.CommentLikeDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.UUID;

public interface CommentLikeApi {

    @Operation(summary = "관심사 댓글 좋아요", description = "댓글 좋아요를 등록합니다.")
    ResponseEntity<CommentLikeDto> create(

            @Parameter(description = "댓글 ID")
            @PathVariable
            UUID commentId,

            @Parameter(description = "요청자 ID")
            @RequestHeader("Monew-Request-User_ID")
            UUID requestUserId
    );

    @Operation(summary = "댓글 좋아요 취소", description = "댓글 좋아요를 취소합니다.")
    ResponseEntity<Void> delete(

            @Parameter(description = "댓글 ID")
            @PathVariable
            UUID commentId,

            @Parameter(description = "요청자 ID")
            @RequestHeader("Monew-Request-User_ID")
            UUID requestUserId
    );
}
