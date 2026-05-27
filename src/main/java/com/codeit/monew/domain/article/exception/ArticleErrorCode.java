package com.codeit.monew.domain.article.exception;

import com.codeit.monew.global.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ArticleErrorCode implements ErrorCode {

    INVALID_ARTICLE_SEARCH_CONDITION(
            HttpStatus.BAD_REQUEST,
            "INVALID_ARTICLE_SEARCH_CONDITION",
            "뉴스 기사 검색 조건이 올바르지 않습니다."
    ),

    ARTICLE_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "ARTICLE_NOT_FOUND",
            "뉴스 기사 정보가 없습니다."
    );

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}