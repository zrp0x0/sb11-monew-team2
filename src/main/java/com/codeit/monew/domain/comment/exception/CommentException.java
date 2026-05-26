package com.codeit.monew.domain.comment.exception;

import com.codeit.monew.global.error.CustomException;
import com.codeit.monew.global.error.ErrorCode;

public class CommentException extends CustomException {

  public CommentException(ErrorCode errorCode) {
    super(errorCode);
  }
}
