package com.codeit.monew.global.error;

import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 도메인 용
    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ErrorResponse> handleCustomException(CustomException e) {
        ErrorCode errorCode = e.getErrorCode();
        log.warn("[CustomException] Code: {}, Message: {}", errorCode.getCode(), errorCode.getMessage());
        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(ErrorResponse.of(e));
    }

    // @RequestBody / @ModelAttribute 용 Validation BindException
    @ExceptionHandler(BindException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(BindException e) {
        Map<String, Object> details = e.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        FieldError::getDefaultMessage,
                        (existingMessage, newMessage) -> existingMessage + ", " + newMessage
                ));
        DomainException domainException = new DomainException(GlobalErrorCode.INVALID_INPUT_VALUE,
                details);
        ErrorCode errorCode = domainException.getErrorCode();
        log.warn("[ValidationException] Code:{} Message: {}", errorCode.getCode(),
                errorCode.getMessage());
        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(ErrorResponse.of(domainException));
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ErrorResponse> handleMissingRequestHeaderException(MissingRequestHeaderException e) {
        log.warn("[MissingRequestHeaderException] HeaderName: {}", e.getHeaderName());
        return ResponseEntity
            .status(GlobalErrorCode.MISSING_REQUEST_HEADER.getHttpStatus())
            .body(ErrorResponse.of(GlobalErrorCode.MISSING_REQUEST_HEADER, e));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatch(MethodArgumentTypeMismatchException e) {
        log.warn("[MethodArgumentTypeMismatchException] Parameter: {}", e.getName());
        return ResponseEntity
            .status(GlobalErrorCode.INVALID_UUID_FORMAT.getHttpStatus())
            .body(ErrorResponse.of(GlobalErrorCode.INVALID_UUID_FORMAT, e));
    }

    // INTERNAL_SERVER_ERROR
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e) {
        log.error("[UnhandledException] Message: {}", e.getMessage(), e);
        return ResponseEntity
                .status(GlobalErrorCode.INTERNAL_SERVER_ERROR.getHttpStatus())
                .body(ErrorResponse.of(GlobalErrorCode.INTERNAL_SERVER_ERROR, e));
    }
}
