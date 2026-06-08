package com.codeit.monew.domain.comment.repository;

import com.codeit.monew.domain.comment.dto.CommentDto;
import com.codeit.monew.domain.comment.dto.CommentSearchRequest;
import com.codeit.monew.global.dto.CursorPageResponse;
import java.util.UUID;

public interface CommentRepositoryCustom {

  CursorPageResponse<CommentDto> findComments(CommentSearchRequest request, UUID requestUserId);

}
