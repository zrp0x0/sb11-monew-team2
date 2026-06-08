package com.codeit.monew.domain.comment.repository;

import com.codeit.monew.domain.comment.dto.CommentDto;
import com.codeit.monew.domain.comment.dto.CommentOrderBy;
import com.codeit.monew.domain.comment.dto.CommentSearchRequest;
import com.codeit.monew.domain.comment.dto.SortDirection;
import com.codeit.monew.domain.comment.entity.Comment;
import com.codeit.monew.domain.comment.entity.QComment;
import com.codeit.monew.global.dto.CursorPageResponse;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

@Slf4j
@Repository
@RequiredArgsConstructor
public class CommentRepositoryImpl implements  CommentRepositoryCustom {

  private final JPAQueryFactory queryFactory;
  private static final DateTimeFormatter CURSOR_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSS");

  @Override
  public CursorPageResponse<CommentDto> findComments(CommentSearchRequest request, UUID requestUserId) {
    QComment comment = QComment.comment;

    CommentOrderBy orderBy = request.getOrderBy();
    boolean isDesc = request.getDirection() == SortDirection.DESC;

    List<Comment> comments = queryFactory
        .selectFrom(comment)
        .join(comment.user).fetchJoin()
        .join(comment.article).fetchJoin()
        .where(
            isNotDeleted(comment),
            articleIdEq(comment, request.articleId()),
            cursorCondition(comment, orderBy, isDesc, request.cursor(), request.after())
        )
        .orderBy(createOrderSpecifier(comment, orderBy, isDesc))
        .limit(request.getLimit() + 1)
        .fetch();

    boolean hasNext = comments.size() > request.getLimit();
    String nextCursor = null;
    String nextAfter = null;

    if(hasNext) {
      comments.remove(comments.size() - 1);
    }

    if(hasNext && !comments.isEmpty()) {
      Comment lastComment = comments.get(comments.size() - 1);
      nextAfter = lastComment.getCreatedAt().format(CURSOR_FORMATTER);

      if(orderBy == CommentOrderBy.LIKE_COUNT) {
        nextCursor = lastComment.getLikeCounts() + "_" + lastComment.getId();
      } else {
        nextCursor = lastComment.getId().toString();
      }
    }

    Long totalElement = null;
    if(!StringUtils.hasText(request.cursor())) {
      totalElement = Optional.ofNullable(
          queryFactory
              .select(comment.count())
              .from(comment)
              .where(isNotDeleted(comment), articleIdEq(comment, request.articleId()))
              .fetchOne()
      ).orElse(0L);
    }

    List<CommentDto> commentDtos = comments.stream()
        .map(c -> CommentDto.of(
            c,
            c.getUser().getNickname(),
            false
        ))
        .collect(Collectors.toList());

    return new CursorPageResponse<>(
        commentDtos,
        nextCursor,
        nextAfter,
        comments.size(),
        totalElement,
        hasNext
    );
  }

  private BooleanExpression isNotDeleted(QComment comment) {
    return comment.deletedAt.isNull();
  }

  private BooleanExpression articleIdEq(QComment comment, UUID articleId) {
    return comment.article.id.eq(articleId);
  }

  private BooleanExpression cursorCondition(
      QComment comment, CommentOrderBy orderBy, boolean isDesc, String cursor, LocalDateTime after) {
    if(!StringUtils.hasText(cursor) || "null".equalsIgnoreCase(cursor.trim()) || "undefined".equalsIgnoreCase(cursor.trim())) {
      return null;
    }

    try {
      String[] parts = cursor.split("_");

      if(orderBy == CommentOrderBy.LIKE_COUNT) {
        if(parts.length < 2) {
          return Expressions.asBoolean(true).isFalse();
        }

        Integer cursorLikeCount = Integer.valueOf(parts[0]);
        UUID cursorId = UUID.fromString(parts[1]);

        if(isDesc) {
          return comment.likeCounts.lt(cursorLikeCount)
              .or(comment.likeCounts.eq(cursorLikeCount)
                  .and(comment.createdAt.lt(after)))
              .or(comment.likeCounts.eq(cursorLikeCount)
                  .and(comment.createdAt.eq(after))
                  .and(comment.id.lt(cursorId)));
        } else {
          return comment.likeCounts.gt(cursorLikeCount)
              .or(comment.likeCounts.eq(cursorLikeCount)
                  .and(comment.createdAt.gt(after)))
              .or(comment.likeCounts.eq(cursorLikeCount)
                  .and(comment.createdAt.eq(after))
                  .and(comment.id.gt(cursorId)));
        }
      } else {

        UUID cursorId = UUID.fromString(parts[0]);

        if(isDesc) {
          return comment.createdAt.lt(after)
              .or(comment.createdAt.eq(after)
                  .and(comment.id.lt(cursorId)));
        } else {
          return comment.createdAt.gt(after)
              .or(comment.createdAt.eq(after)
                  .and(comment.id.gt(cursorId)));
        }
      }
    } catch (Exception e) {
      return Expressions.asBoolean(true).isFalse();
    }
  }

  private OrderSpecifier<?>[] createOrderSpecifier(
      QComment comment, CommentOrderBy orderBy, boolean isDesc) {
    if(orderBy == CommentOrderBy.LIKE_COUNT) {
      return new OrderSpecifier[]{
          isDesc ? comment.likeCounts.desc() : comment.likeCounts.asc(),
          isDesc ? comment.createdAt.desc() : comment.createdAt.asc(),
          isDesc ? comment.id.desc() : comment.id.asc()
      };
    } else {
      return new OrderSpecifier[] {
          isDesc ? comment.createdAt.desc() : comment.createdAt.asc(),
          isDesc ? comment.id.desc() : comment.id.asc()
      };
    }
  }
}
