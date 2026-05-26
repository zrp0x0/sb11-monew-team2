package com.codeit.monew.domain.comment.service;

import com.codeit.monew.domain.article.entity.Article;
import com.codeit.monew.domain.article.repository.ArticleRepository;
import com.codeit.monew.domain.comment.dto.CommentDto;
import com.codeit.monew.domain.comment.dto.CommentRegisterRequest;
import com.codeit.monew.domain.comment.entity.Comment;
import com.codeit.monew.domain.comment.exception.CommentErrorCode;
import com.codeit.monew.domain.comment.exception.CommentException;
import com.codeit.monew.domain.comment.repository.CommentRepository;
import com.codeit.monew.domain.user.entity.User;
import com.codeit.monew.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CommentService {

  private final CommentRepository commentRepository;
  private final ArticleRepository articleRepository;
  private final UserRepository userRepository;

  public CommentDto createComment(CommentRegisterRequest request) {
    Article article = articleRepository.findById(request.articleId())
        .orElseThrow(() -> new CommentException(CommentErrorCode.ARTICLE_NOT_FOUND));

    User user = userRepository.findById(request.userId())
        .orElseThrow(() -> new CommentException(CommentErrorCode.USER_NOT_FOUND));

    Comment saved = commentRepository.save(
        Comment.create(article, user, request.content())
    );

    return CommentDto.of(saved, user.getNickname(), false);
  }
}
