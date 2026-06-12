package com.codeit.monew.batch.exception;

import com.codeit.monew.global.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum BatchErrorCode implements ErrorCode {

  INVALID_DATE_RANGE(HttpStatus.BAD_REQUEST, "INVALID_DATE_RANGE", "요청된 날짜 범위가 올바르지 않습니다."),

  BACKUP_JOB_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "BACKUP_JOB_FAILED", "기사 백업 배치 실행 중 오류가 발생했습니다.");

  private final HttpStatus httpStatus;
  private final String code;
  private final String message;

}
