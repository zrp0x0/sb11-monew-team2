package com.codeit.monew.batch.delete.service;

import com.codeit.monew.domain.comment.repository.CommentRepository;
import com.codeit.monew.domain.commentLike.repository.CommentLikeRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CommentHardDeleteChunkService {

  private final CommentRepository commentRepository;
  private final CommentLikeRepository commentLikeRepository;

  @Transactional
  public void deleteChunk(List<UUID> ids) {
    commentLikeRepository.hardDeleteAllByCommentIdIn(ids);
    commentRepository.hardDeleteAllByIdIn(ids);
  }
}
