package com.codeit.monew.global.error;

import java.util.Map;

public class DomainException extends CustomException {

    public DomainException(ErrorCode errorCode) {
        super(errorCode);
    }

    public DomainException(ErrorCode errorCode, Map<String, Object> details) {
        super(errorCode, details);
    }
}
