package com.codeit.monew.domain.user.exception;

import com.codeit.monew.global.error.CustomException;
import com.codeit.monew.global.error.ErrorCode;
import java.util.Map;

public class UserException extends CustomException {

    public UserException(ErrorCode errorCode) {
        super(errorCode);
    }

    public UserException(ErrorCode errorCode, Map<String, Object> details) {
        super(errorCode, details);
    }
}
