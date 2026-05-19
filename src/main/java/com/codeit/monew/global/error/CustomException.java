package com.codeit.monew.global.error;

import java.util.Collections;
import java.util.Map;
import lombok.Getter;

@Getter
public abstract class CustomException extends RuntimeException {

    private final ErrorCode errorCode;
    private final Map<String, Object> details;

    protected CustomException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.details = Collections.emptyMap();
    }

    protected CustomException(ErrorCode errorCode, Map<String, Object> details) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.details = details;
    }
}
