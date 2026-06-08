package com.codeit.monew.domain.article.repository;

import static com.codeit.monew.domain.article.entity.QArticle.article;
import static com.codeit.monew.domain.article.entity.QArticleInterest.articleInterest;

import com.codeit.monew.domain.article.dto.request.ArticleSearchRequest;
import com.codeit.monew.domain.article.entity.Article;
import com.codeit.monew.domain.article.entity.ArticleSource;
import com.codeit.monew.global.dto.CursorPageResponse;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

@Repository
@RequiredArgsConstructor
public class ArticleRepositoryImpl implements ArticleRepositoryCustom {

  private final JPAQueryFactory queryFactory;
  private static final DateTimeFormatter CURSOR_DATE_FORMATTER = DateTimeFormatter.ofPattern(
      "yyyy-MM-dd'T'HH:mm:ss.SSSSSS");

  @Override
  public CursorPageResponse<Article> searchArticles(ArticleSearchRequest request) {

    String orderBy =
        StringUtils.hasText(request.orderBy()) ? request.orderBy().trim() : "publishDate";
    String direction =
        StringUtils.hasText(request.direction()) ? request.direction().trim() : "DESC";

    // 목록 조회
    List<Article> articles = queryFactory
        .selectFrom(article)
        .where(
            isNotDeleted(),
            searchInterestId(request.interestId()),
            searchKeyword(request.keyword()),
            searchSourceIn(request.sourceIn()),
            searchPublishDate(request.publishDateFrom(), request.publishDateTo()),
            cursorCondition(orderBy, direction, request.cursor(), request.after())
        )
        .orderBy(createOrderSpecifier(orderBy, direction))
        .limit(request.limit() + 1)
        .fetch();

    // 다음 페이지 존재 여부 확인 및 커서 결합
    boolean hasNext = articles.size() > request.limit();
    String nextCursor = null;
    LocalDateTime nextAfter = null;

    if (hasNext) {
      articles.remove(articles.size() - 1);
    }

    if (hasNext && !articles.isEmpty()) {
      Article lastArticle = articles.get(articles.size() - 1);

      nextAfter = lastArticle.getCreatedAt();

      if ("publishDate".equals(orderBy)) {
        String formattedPubDate = lastArticle.getPublishedAt()
            .format(CURSOR_DATE_FORMATTER);
        nextCursor = formattedPubDate + "_" + lastArticle.getId();
      } else if ("commentCount".equals(orderBy)) {
        nextCursor = lastArticle.getCommentCount() + "_" + lastArticle.getId();
      } else {
        nextCursor = lastArticle.getViewCount() + "_" + lastArticle.getId();
      }
    }

    // 첫 페이지 조회 시에만 카운트 쿼리 실행
    Long totalElementCount = null;
    if (!StringUtils.hasText(request.cursor()) || "null".equalsIgnoreCase(
        request.cursor().trim()) || "undefined".equalsIgnoreCase(request.cursor().trim())) {
      totalElementCount = Optional.ofNullable(
          queryFactory
              .select(article.count())
              .from(article)
              .where(
                  isNotDeleted(),
                  searchInterestId(request.interestId()),
                  searchKeyword(request.keyword()),
                  searchSourceIn(request.sourceIn()),
                  searchPublishDate(request.publishDateFrom(), request.publishDateTo())
              )
              .fetchOne()
      ).orElse(0L);
    }

    return new CursorPageResponse<>(
        articles,
        nextCursor,
        nextAfter != null ? nextAfter.format(CURSOR_DATE_FORMATTER) : null,
        request.limit(),
        totalElementCount,
        hasNext
    );
  }

  private BooleanExpression isNotDeleted() {
    return article.isDeleted.isFalse();
  }

  private BooleanExpression searchInterestId(UUID interestId) {
    if (interestId == null) {
      return null;
    }

    return JPAExpressions.selectOne()
        .from(articleInterest)
        .where(
            articleInterest.article.eq(article),
            articleInterest.interest.id.eq(interestId)
        )
        .exists();
  }

  private BooleanExpression searchKeyword(String keyword) {
    if (!StringUtils.hasText(keyword)) {
      return null;
    }
    return article.title.containsIgnoreCase(keyword)
        .or(article.summary.containsIgnoreCase(keyword));
  }

  private BooleanExpression searchSourceIn(List<ArticleSource> sources) {
    if (sources == null || sources.isEmpty()) {
      return null;
    }
    return article.source.in(sources);
  }

  private BooleanExpression searchPublishDate(LocalDateTime from, LocalDateTime to) {
    BooleanExpression condition = null;
    if (from != null) {
      condition = article.publishedAt.goe(from);
    }
    if (to != null) {
      condition = condition != null ? condition.and(article.publishedAt.loe(to))
          : article.publishedAt.loe(to);
    }
    return condition;
  }


  private BooleanExpression cursorCondition(String orderBy, String direction, String cursor,
      LocalDateTime after) {
    if (!StringUtils.hasText(cursor) || "null".equalsIgnoreCase(cursor.trim())
        || "undefined".equalsIgnoreCase(cursor.trim())) {
      return null;
    }

    boolean isAsc = "ASC".equalsIgnoreCase(direction);

    try {
      String[] parts = cursor.split("_");
      String primaryStr = parts[0];     // 1정렬 기준값 (날짜 문자열 또는 숫자)
      UUID cursorId = UUID.fromString(parts[1]);
      LocalDateTime cursorCreatedAt = after;

      // 발행일(publishDate) 기준 페이징
      if ("publishDate".equals(orderBy)) {
        LocalDateTime cursorPublishDate = LocalDateTime.parse(primaryStr,
            CURSOR_DATE_FORMATTER);

        if (isAsc) {
          return article.publishedAt.gt(cursorPublishDate)
              .or(article.publishedAt.eq(cursorPublishDate)
                  .and(article.createdAt.gt(cursorCreatedAt)))
              .or(article.publishedAt.eq(cursorPublishDate)
                  .and(article.createdAt.eq(cursorCreatedAt))
                  .and(article.id.gt(cursorId)));
        } else {
          return article.publishedAt.lt(cursorPublishDate)
              .or(article.publishedAt.eq(cursorPublishDate)
                  .and(article.createdAt.lt(cursorCreatedAt)))
              .or(article.publishedAt.eq(cursorPublishDate)
                  .and(article.createdAt.eq(cursorCreatedAt))
                  .and(article.id.lt(cursorId)));
        }
      }

      // 댓글 수(commentCount) 기준 페이징
      else if ("commentCount".equals(orderBy)) {
        Long cursorCommentCount = Long.valueOf(primaryStr);

        if (isAsc) {
          return article.commentCount.gt(cursorCommentCount)
              .or(article.commentCount.eq(cursorCommentCount)
                  .and(article.createdAt.gt(cursorCreatedAt)))
              .or(article.commentCount.eq(cursorCommentCount)
                  .and(article.createdAt.eq(cursorCreatedAt))
                  .and(article.id.gt(cursorId)));
        } else {
          return article.commentCount.lt(cursorCommentCount)
              .or(article.commentCount.eq(cursorCommentCount)
                  .and(article.createdAt.lt(cursorCreatedAt)))
              .or(article.commentCount.eq(cursorCommentCount)
                  .and(article.createdAt.eq(cursorCreatedAt))
                  .and(article.id.lt(cursorId)));
        }
      }

      // 조회수(viewCount) 기준 페이징
      else {
        Long cursorViewCount = Long.valueOf(primaryStr);

        if (isAsc) {
          return article.viewCount.gt(cursorViewCount)
              .or(article.viewCount.eq(cursorViewCount)
                  .and(article.createdAt.gt(cursorCreatedAt)))
              .or(article.viewCount.eq(cursorViewCount)
                  .and(article.createdAt.eq(cursorCreatedAt))
                  .and(article.id.gt(cursorId)));
        } else {
          return article.viewCount.lt(cursorViewCount)
              .or(article.viewCount.eq(cursorViewCount)
                  .and(article.createdAt.lt(cursorCreatedAt)))
              .or(article.viewCount.eq(cursorViewCount)
                  .and(article.createdAt.eq(cursorCreatedAt))
                  .and(article.id.lt(cursorId)));
        }
      }
    } catch (Exception e) {
      return Expressions.asBoolean(true).isFalse();
    }
  }

  private OrderSpecifier<?>[] createOrderSpecifier(String orderBy, String direction) {
    boolean isAsc = "ASC".equalsIgnoreCase(direction);
    Order order = isAsc ? Order.ASC : Order.DESC;

    if ("publishDate".equals(orderBy)) {
      return new OrderSpecifier[]{
          new OrderSpecifier<>(order, article.publishedAt),
          new OrderSpecifier<>(order, article.createdAt),
          new OrderSpecifier<>(order, article.id)
      };
    } else if ("commentCount".equals(orderBy)) {
      return new OrderSpecifier[]{
          new OrderSpecifier<>(order, article.commentCount),
          new OrderSpecifier<>(order, article.createdAt),
          new OrderSpecifier<>(order, article.id)
      };
    } else {
      return new OrderSpecifier[]{
          new OrderSpecifier<>(order, article.viewCount),
          new OrderSpecifier<>(order, article.createdAt),
          new OrderSpecifier<>(order, article.id)
      };
    }
  }
}