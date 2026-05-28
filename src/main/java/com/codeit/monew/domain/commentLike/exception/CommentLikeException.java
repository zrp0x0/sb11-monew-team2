package com.codeit.monew.domain.commentLike.exception;

import com.codeit.monew.global.error.CustomException;
import com.codeit.monew.global.error.ErrorCode;

public class CommentLikeException extends CustomException {

    public CommentLikeException(ErrorCode errorCode) {
        super(errorCode);
    }
}
