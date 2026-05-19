package com.codeit.monew.domain.user.exception;

import com.codeit.monew.global.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum UserErrorCode implements ErrorCode {

    EMAIL_DUPLICATION(HttpStatus.CONFLICT, "EMAIL_DUPLICATION", "이미 존재하는 이메일입니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
