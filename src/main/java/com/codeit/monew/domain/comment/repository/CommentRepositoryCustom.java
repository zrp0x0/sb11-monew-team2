package com.codeit.monew.domain.comment.repository;

import com.codeit.monew.domain.comment.dto.CommentSearchRequest;
import com.codeit.monew.domain.comment.dto.CursorPageResponseCommentDto;
import java.util.UUID;

public interface CommentRepositoryCustom {

  CursorPageResponseCommentDto findComments(CommentSearchRequest request, UUID requestUserId);

}
