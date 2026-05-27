package com.codeit.monew.domain.interest.exception;

import com.codeit.monew.global.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum InterestErrorCode implements ErrorCode {

  INTEREST_NOT_FOUND(HttpStatus.NOT_FOUND, "INTEREST_NOT_FOUND", "관심사를 찾을 수 없습니다."),

  INTEREST_ALREADY_EXISTS(HttpStatus.CONFLICT, "INTEREST_ALREADY_EXISTS", "이미 존재하는 관심사입니다."),

  SIMILAR_INTEREST_EXISTS(HttpStatus.CONFLICT, "SIMILAR_INTEREST_EXISTS", "유사한 관심사가 존재합니다.");

  private final HttpStatus httpStatus;
  private final String code;
  private final String message;

}
