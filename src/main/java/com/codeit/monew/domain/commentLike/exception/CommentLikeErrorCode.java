package com.codeit.monew.domain.commentLike.exception;

import com.codeit.monew.global.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum CommentLikeErrorCode implements ErrorCode {

    COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "COMMENT_NOT_FOUND", "댓글을 찾을 수 없습니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "사용자를 찾을 수 없습니다."),
    COMMENT_LIKE_ALREADY_EXISTS(HttpStatus.BAD_REQUEST, "COMMENT_LIKE_ALREADY_EXISTS", "해당 댓글에 이미 좋아요를 남겼습니다."),
    COMMENT_LIKE_NOT_FOUND(HttpStatus.NOT_FOUND, "COMMENT_LIKE_NOT_FOUND", "해당 댓글에 좋아요를 남기지 않았습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
