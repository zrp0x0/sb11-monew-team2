package com.codeit.monew.domain.comment.exception;

import com.codeit.monew.global.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum CommentErrorCode implements ErrorCode {

  ARTICLE_NOT_FOUND(HttpStatus.NOT_FOUND, "ARTICLE_NOT_FOUND","게시글을 찾을 수 없습니다."),
  USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "유저를 찾을 수 없습니다."),
  COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "COMMENT_NOT_FOUND", "댓글을 찾을 수 없습니다."),

  COMMENT_UNAUTHORIZED(HttpStatus.FORBIDDEN, "COMMENT_UNAUTHORIZED", "댓글 작성자만 수정할 수 있습니다."),

  MISSING_USER_ID(HttpStatus.BAD_REQUEST, "MISSING_USER_ID", "사용자 ID 헤더가 누락되었습니다."),
  INVALID_UUID_FORMAT(HttpStatus.BAD_REQUEST, "INVALID_UUID_FORMAT", "UUID 형식이 올바르지 않습니다."),

  INVALID_CURSOR_PARAMETER(HttpStatus.BAD_REQUEST, "INVALID_CURSOR_PARAMETER", "잘못된 페이지네이션 파라미터입니다.");

  private final HttpStatus httpStatus;
  private final String code;
  private final String message;
}
