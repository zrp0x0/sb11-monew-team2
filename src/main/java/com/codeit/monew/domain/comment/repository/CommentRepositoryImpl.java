package com.codeit.monew.domain.comment.repository;

import com.codeit.monew.domain.comment.dto.CommentOrderBy;
import com.codeit.monew.domain.comment.entity.Comment;
import com.codeit.monew.domain.comment.entity.QComment;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class CommentRepositoryImpl implements  CommentRepositoryCustom {

  private final JPAQueryFactory queryFactory;

  @Override
  public List<Comment> findComments(
      UUID articleId,
      CommentOrderBy orderBy,
      LocalDateTime cursorCreatedAt,
      UUID cursorId,
      Integer cursorLikeCount,
      int limit
  ) {
    QComment comment = QComment.comment;
    BooleanBuilder where = new BooleanBuilder();

    where.and(comment.article.id.eq(articleId));

    if(orderBy == CommentOrderBy.createdAt && cursorCreatedAt != null && cursorId != null) {
      where.and(
          comment.createdAt.lt(cursorCreatedAt)
              .or(comment.createdAt.eq(cursorCreatedAt)
              .and(comment.id.lt(cursorId)))
      );
    } else if (orderBy == CommentOrderBy.likeCount && cursorLikeCount != null && cursorCreatedAt != null) {
      where.and(
          comment.likeCounts.lt(cursorLikeCount)
              .or(comment.likeCounts.eq(cursorLikeCount)
              .and(comment.createdAt.lt(cursorCreatedAt)))
      );
    }

    OrderSpecifier<?>[] orderBy_ = (orderBy == CommentOrderBy.createdAt)
        ? new OrderSpecifier[]{comment.createdAt.desc(), comment.id.desc()}
        : new OrderSpecifier[]{comment.likeCounts.desc(), comment.createdAt.desc()};

    return queryFactory
        .selectFrom(comment)
        .join(comment.user).fetchJoin()
        .where(where)
        .orderBy(orderBy_)
        .limit(limit)
        .fetch();
  }

}
