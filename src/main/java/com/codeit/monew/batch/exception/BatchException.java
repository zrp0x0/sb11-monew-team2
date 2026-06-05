package com.codeit.monew.batch.exception;

import com.codeit.monew.global.error.CustomException;
import com.codeit.monew.global.error.ErrorCode;
import java.util.Map;

public class BatchException extends CustomException {

  public BatchException(ErrorCode errorCode) {
    super(errorCode);
  }

  public BatchException(ErrorCode errorCode, Map<String, Object> details) {
    super(errorCode, details);
  }
}
