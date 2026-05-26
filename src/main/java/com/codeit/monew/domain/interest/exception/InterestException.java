package com.codeit.monew.domain.interest.exception;

import com.codeit.monew.global.error.CustomException;
import com.codeit.monew.global.error.ErrorCode;
import java.util.Map;

public class InterestException extends CustomException {

  public InterestException(ErrorCode errorCode) {
    super(errorCode);
  }

  public InterestException(ErrorCode errorCode, Map<String, Object> details) {
    super(errorCode, details);
  }
}
