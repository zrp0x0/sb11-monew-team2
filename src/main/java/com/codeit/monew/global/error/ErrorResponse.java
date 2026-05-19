package com.codeit.monew.global.error;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;

public record ErrorResponse(
        LocalDateTime timestamp,
        Integer status,
        String code,
        String message,
        Map<String, Object> details,
        String exceptionType
) {

    public static ErrorResponse of(CustomException e) {
        return new ErrorResponse(
                LocalDateTime.now(),
                e.getErrorCode().getHttpStatus().value(),
                e.getErrorCode().getCode(),
                e.getErrorCode().getMessage(),
                e.getDetails(),
                e.getClass().getSimpleName()
        );
    }

    public static ErrorResponse of(ErrorCode errorCode, Exception e) {
        return new ErrorResponse(
                LocalDateTime.now(),
                errorCode.getHttpStatus().value(),
                errorCode.getCode(),
                errorCode.getMessage(),
                Collections.emptyMap(),
                e.getClass().getSimpleName()
        );
    }
}
