package com.codeit.monew.domain.notification.exception;

import com.codeit.monew.global.error.CustomException;
import com.codeit.monew.global.error.ErrorCode;
import java.util.Map;

public class NotificationException extends CustomException {

    public NotificationException(ErrorCode errorCode) {
        super(errorCode);
    }

    public NotificationException(ErrorCode errorCode, Map<String, Object> details) {
        super(errorCode, details);
    }
}
