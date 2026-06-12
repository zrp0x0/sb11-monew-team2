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

  private static final DateTimeFormatter CURSOR_DATE_FORMATTER = DateTimeFormatter.ofPattern(
          "yyyy-MM-dd'T'HH:mm:ss.SSSSSS"
  );

  private static final String ORDER_BY_PUBLISH_DATE = "publishDate";
  private static final String ORDER_BY_COMMENT_COUNT = "commentCount";
  private static final String ORDER_BY_VIEW_COUNT = "viewCount";
  private static final String DEFAULT_ORDER_BY = ORDER_BY_PUBLISH_DATE;
  private static final String DEFAULT_DIRECTION = "DESC";
  private static final String ASC = "ASC";

  private final JPAQueryFactory queryFactory;

  @Override
  public CursorPageResponse<Article> searchArticles(ArticleSearchRequest request) {
    String orderBy = resolveOrderBy(request.orderBy());
    String direction = resolveDirection(request.direction());
    int limit = request.limit();

    List<Article> articles = fetchArticles(request, orderBy, direction, limit);
    boolean hasNext = articles.size() > limit;

    if (hasNext) {
      articles.remove(articles.size() - 1);
    }

    PageCursor pageCursor = createPageCursor(articles, orderBy, hasNext);

    Long totalElementCount = isInitialCursor(request.cursor())
            ? countArticles(request)
            : null;

    return new CursorPageResponse<>(
            articles,
            pageCursor.nextCursor(),
            pageCursor.nextAfter(),
            limit,
            totalElementCount,
            hasNext
    );
  }

  private List<Article> fetchArticles(
          ArticleSearchRequest request,
          String orderBy,
          String direction,
          int limit
  ) {
    return queryFactory
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
            .limit(limit + 1)
            .fetch();
  }

  private Long countArticles(ArticleSearchRequest request) {
    return Optional.ofNullable(
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

  private PageCursor createPageCursor(List<Article> articles, String orderBy, boolean hasNext) {
    if (!hasNext || articles.isEmpty()) {
      return new PageCursor(null, null);
    }

    Article lastArticle = articles.get(articles.size() - 1);

    return new PageCursor(
            createNextCursor(lastArticle, orderBy),
            lastArticle.getCreatedAt().format(CURSOR_DATE_FORMATTER)
    );
  }

  private String createNextCursor(Article lastArticle, String orderBy) {
    if (ORDER_BY_PUBLISH_DATE.equals(orderBy)) {
      return lastArticle.getPublishedAt().format(CURSOR_DATE_FORMATTER)
              + "_"
              + lastArticle.getId();
    }

    if (ORDER_BY_COMMENT_COUNT.equals(orderBy)) {
      return lastArticle.getCommentCount() + "_" + lastArticle.getId();
    }

    return lastArticle.getViewCount() + "_" + lastArticle.getId();
  }

  private String resolveOrderBy(String orderBy) {
    return StringUtils.hasText(orderBy) ? orderBy.trim() : DEFAULT_ORDER_BY;
  }

  private String resolveDirection(String direction) {
    return StringUtils.hasText(direction) ? direction.trim() : DEFAULT_DIRECTION;
  }

  private boolean isInitialCursor(String cursor) {
    return !StringUtils.hasText(cursor)
            || "null".equalsIgnoreCase(cursor.trim())
            || "undefined".equalsIgnoreCase(cursor.trim());
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
      condition = condition != null
              ? condition.and(article.publishedAt.loe(to))
              : article.publishedAt.loe(to);
    }

    return condition;
  }

  private BooleanExpression cursorCondition(
          String orderBy,
          String direction,
          String cursor,
          LocalDateTime after
  ) {
    if (isInitialCursor(cursor)) {
      return null;
    }

    boolean isAsc = ASC.equalsIgnoreCase(direction);

    try {
      CursorParts cursorParts = parseCursor(cursor);

      if (ORDER_BY_PUBLISH_DATE.equals(orderBy)) {
        return publishDateCursorCondition(cursorParts, after, isAsc);
      }

      if (ORDER_BY_COMMENT_COUNT.equals(orderBy)) {
        return commentCountCursorCondition(cursorParts, after, isAsc);
      }

      return viewCountCursorCondition(cursorParts, after, isAsc);
    } catch (Exception e) {
      // 잘못된 cursor가 들어오면 기존 동작처럼 빈 결과가 반환되도록 false 조건을 반환한다.
      return Expressions.asBoolean(true).isFalse();
    }
  }

  private CursorParts parseCursor(String cursor) {
    String[] parts = cursor.split("_");
    return new CursorParts(parts[0], UUID.fromString(parts[1]));
  }

  private BooleanExpression publishDateCursorCondition(
          CursorParts cursorParts,
          LocalDateTime cursorCreatedAt,
          boolean isAsc
  ) {
    LocalDateTime cursorPublishDate = LocalDateTime.parse(
            cursorParts.primaryValue(),
            CURSOR_DATE_FORMATTER
    );

    if (isAsc) {
      return article.publishedAt.gt(cursorPublishDate)
              .or(article.publishedAt.eq(cursorPublishDate)
                      .and(article.createdAt.gt(cursorCreatedAt)))
              .or(article.publishedAt.eq(cursorPublishDate)
                      .and(article.createdAt.eq(cursorCreatedAt))
                      .and(article.id.gt(cursorParts.articleId())));
    }

    return article.publishedAt.lt(cursorPublishDate)
            .or(article.publishedAt.eq(cursorPublishDate)
                    .and(article.createdAt.lt(cursorCreatedAt)))
            .or(article.publishedAt.eq(cursorPublishDate)
                    .and(article.createdAt.eq(cursorCreatedAt))
                    .and(article.id.lt(cursorParts.articleId())));
  }

  private BooleanExpression commentCountCursorCondition(
          CursorParts cursorParts,
          LocalDateTime cursorCreatedAt,
          boolean isAsc
  ) {
    Long cursorCommentCount = Long.valueOf(cursorParts.primaryValue());

    if (isAsc) {
      return article.commentCount.gt(cursorCommentCount)
              .or(article.commentCount.eq(cursorCommentCount)
                      .and(article.createdAt.gt(cursorCreatedAt)))
              .or(article.commentCount.eq(cursorCommentCount)
                      .and(article.createdAt.eq(cursorCreatedAt))
                      .and(article.id.gt(cursorParts.articleId())));
    }

    return article.commentCount.lt(cursorCommentCount)
            .or(article.commentCount.eq(cursorCommentCount)
                    .and(article.createdAt.lt(cursorCreatedAt)))
            .or(article.commentCount.eq(cursorCommentCount)
                    .and(article.createdAt.eq(cursorCreatedAt))
                    .and(article.id.lt(cursorParts.articleId())));
  }

  private BooleanExpression viewCountCursorCondition(
          CursorParts cursorParts,
          LocalDateTime cursorCreatedAt,
          boolean isAsc
  ) {
    Long cursorViewCount = Long.valueOf(cursorParts.primaryValue());

    if (isAsc) {
      return article.viewCount.gt(cursorViewCount)
              .or(article.viewCount.eq(cursorViewCount)
                      .and(article.createdAt.gt(cursorCreatedAt)))
              .or(article.viewCount.eq(cursorViewCount)
                      .and(article.createdAt.eq(cursorCreatedAt))
                      .and(article.id.gt(cursorParts.articleId())));
    }

    return article.viewCount.lt(cursorViewCount)
            .or(article.viewCount.eq(cursorViewCount)
                    .and(article.createdAt.lt(cursorCreatedAt)))
            .or(article.viewCount.eq(cursorViewCount)
                    .and(article.createdAt.eq(cursorCreatedAt))
                    .and(article.id.lt(cursorParts.articleId())));
  }

  private OrderSpecifier<?>[] createOrderSpecifier(String orderBy, String direction) {
    boolean isAsc = ASC.equalsIgnoreCase(direction);
    Order order = isAsc ? Order.ASC : Order.DESC;

    if (ORDER_BY_PUBLISH_DATE.equals(orderBy)) {
      return new OrderSpecifier<?>[]{
              new OrderSpecifier<>(order, article.publishedAt),
              new OrderSpecifier<>(order, article.createdAt),
              new OrderSpecifier<>(order, article.id)
      };
    }

    if (ORDER_BY_COMMENT_COUNT.equals(orderBy)) {
      return new OrderSpecifier<?>[]{
              new OrderSpecifier<>(order, article.commentCount),
              new OrderSpecifier<>(order, article.createdAt),
              new OrderSpecifier<>(order, article.id)
      };
    }

    return new OrderSpecifier<?>[]{
            new OrderSpecifier<>(order, article.viewCount),
            new OrderSpecifier<>(order, article.createdAt),
            new OrderSpecifier<>(order, article.id)
    };
  }

  private record PageCursor(
          String nextCursor,
          String nextAfter
  ) {
  }

  private record CursorParts(
          String primaryValue,
          UUID articleId
  ) {
  }
}