package com.codeit.monew.domain.user.exception;

import com.codeit.monew.global.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum UserErrorCode implements ErrorCode {

    EMAIL_DUPLICATION(HttpStatus.CONFLICT, "EMAIL_DUPLICATION", "이미 존재하는 이메일입니다."),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "이메일 또는 비밀번호가 올바르지 않습니다."),
    REQUEST_USER_ID_REQUIRED(HttpStatus.UNAUTHORIZED, "REQUEST_USER_ID_REQUIRED",
            "요청자 헤더가 필요합니다."),
    USER_ACCESS_DENIED(HttpStatus.FORBIDDEN, "USER_ACCESS_DENIED", "사용자 접근 권한이 없습니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "사용자를 찾을 수 없습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
