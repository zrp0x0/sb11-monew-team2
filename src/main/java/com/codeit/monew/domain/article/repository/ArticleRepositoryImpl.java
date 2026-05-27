package com.codeit.monew.domain.article.repository;

import static com.codeit.monew.domain.article.entity.QArticle.article;

import com.codeit.monew.domain.article.dto.request.ArticleSearchRequest;
import com.codeit.monew.domain.article.entity.Article;
import com.codeit.monew.domain.article.entity.ArticleSource;
import com.codeit.monew.domain.article.exception.ArticleErrorCode;
import com.codeit.monew.domain.article.exception.ArticleException;
import com.codeit.monew.global.dto.CursorPageResponse;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;

@RequiredArgsConstructor
public class ArticleRepositoryImpl implements ArticleRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public CursorPageResponse<Article> searchArticles(ArticleSearchRequest request) {
        validateLimit(request.limit());

        List<Article> articles = queryFactory
                .selectFrom(article)
                .where(
                        notDeleted(),
                        interestIdEq(request.interestId()),
                        keywordContains(request.keyword()),
                        sourceIn(request.sourceIn()),
                        publishedAtGoe(request.publishDateFrom()),
                        publishedAtLoe(request.publishDateTo()),
                        cursorCondition(request)
                )
                .orderBy(orderSpecifiers(request))
                .limit(request.limit() + 1L)
                .fetch();

        Long totalElements = countTotalElements(request);

        boolean hasNext = articles.size() > request.limit();

        List<Article> content = hasNext
                ? articles.subList(0, request.limit())
                : articles;

        String nextCursor = getNextCursor(content, request.orderBy(), hasNext);
        String nextAfter = getNextAfter(content, hasNext);

        return new CursorPageResponse<>(
                content,
                nextCursor,
                nextAfter,
                content.size(),
                totalElements,
                hasNext
        );
    }

    private BooleanExpression interestIdEq(UUID interestId) {
        // TODO: ArticleInterest 중간 테이블 구조 확정 후 interestId 필터 조건 추가
        return null;
    }

    private void validateLimit(int limit) {
        if (limit <= 0) {
            throw invalidSearchCondition("limit", limit);
        }
    }

    private ArticleException invalidSearchCondition(String field, Object value) {
        return new ArticleException(
                ArticleErrorCode.INVALID_ARTICLE_SEARCH_CONDITION,
                Map.of(field, String.valueOf(value))
        );
    }

    private Long countTotalElements(ArticleSearchRequest request) {
        Long count = queryFactory
                .select(article.count())
                .from(article)
                .where(
                        notDeleted(),
                        interestIdEq(request.interestId()),
                        keywordContains(request.keyword()),
                        sourceIn(request.sourceIn()),
                        publishedAtGoe(request.publishDateFrom()),
                        publishedAtLoe(request.publishDateTo())
                )
                .fetchOne();

        return count == null ? 0L : count;
    }

    private BooleanExpression notDeleted() {
        return article.deletedAt.isNull();
    }

    private BooleanExpression keywordContains(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return null;
        }

        return article.title.containsIgnoreCase(keyword)
                .or(article.summary.containsIgnoreCase(keyword));
    }

    private BooleanExpression sourceIn(List<ArticleSource> sources) {
        if (sources == null || sources.isEmpty()) {
            return null;
        }

        return article.source.in(sources);
    }

    private BooleanExpression publishedAtGoe(LocalDateTime publishDateFrom) {
        if (publishDateFrom == null) {
            return null;
        }

        return article.publishedAt.goe(publishDateFrom);
    }

    private BooleanExpression publishedAtLoe(LocalDateTime publishDateTo) {
        if (publishDateTo == null) {
            return null;
        }

        return article.publishedAt.loe(publishDateTo);
    }

    private OrderSpecifier<?>[] orderSpecifiers(ArticleSearchRequest request) {
        Order order = isAscending(request.direction()) ? Order.ASC : Order.DESC;

        return switch (request.orderBy()) {
            case "publishDate" -> new OrderSpecifier<?>[] {
                    new OrderSpecifier<>(order, article.publishedAt),
                    new OrderSpecifier<>(order, article.createdAt),
                    new OrderSpecifier<>(order, article.id)
            };
            case "commentCount" -> new OrderSpecifier<?>[] {
                    new OrderSpecifier<>(order, article.commentCount),
                    new OrderSpecifier<>(order, article.createdAt),
                    new OrderSpecifier<>(order, article.id)
            };
            case "viewCount" -> new OrderSpecifier<?>[] {
                    new OrderSpecifier<>(order, article.viewCount),
                    new OrderSpecifier<>(order, article.createdAt),
                    new OrderSpecifier<>(order, article.id)
            };
            default -> throw invalidSearchCondition("orderBy", request.orderBy());
        };
    }

    private boolean isAscending(String direction) {
        if ("ASC".equalsIgnoreCase(direction)) {
            return true;
        }

        if ("DESC".equalsIgnoreCase(direction)) {
            return false;
        }

        throw invalidSearchCondition("direction", direction);
    }

    private record ParsedCursor(String value, UUID articleId) {
    }

    private ParsedCursor parseCursor(String cursor) {
        String[] parts = cursor.split("\\|", 2);

        if (parts.length != 2 || !StringUtils.hasText(parts[0]) || !StringUtils.hasText(parts[1])) {
            throw invalidSearchCondition("cursor", cursor);
        }

        try {
            return new ParsedCursor(parts[0], UUID.fromString(parts[1]));
        } catch (IllegalArgumentException e) {
            throw invalidSearchCondition("cursor", cursor);
        }
    }

    private BooleanExpression cursorCondition(ArticleSearchRequest request) {
        if (!StringUtils.hasText(request.cursor())) {
            return null;
        }

        if (request.after() == null) {
            throw invalidSearchCondition("after", null);
        }

        ParsedCursor parsedCursor = parseCursor(request.cursor());
        boolean ascending = isAscending(request.direction());

        try{
            return switch (request.orderBy()) {
                case "publishDate" -> publishDateCursorCondition(
                        LocalDateTime.parse(request.cursor()),
                        request.after(),
                        parsedCursor.articleId(),
                        ascending
                );
                case "commentCount" -> commentCountCursorCondition(
                        Long.valueOf(request.cursor()),
                        request.after(),
                        parsedCursor.articleId(),
                        ascending
                );
                case "viewCount" -> viewCountCursorCondition(
                        Long.valueOf(request.cursor()),
                        request.after(),
                        parsedCursor.articleId(),
                        ascending
                );
                default -> throw invalidSearchCondition("orderBy", request.orderBy());
            };
        } catch (NumberFormatException | DateTimeParseException e) {
            throw invalidSearchCondition("cursor", request.cursor());
        }
    }

    private BooleanExpression publishDateCursorCondition(
            LocalDateTime cursor,
            LocalDateTime after,
            UUID articleId,
            boolean ascending
    ) {
        BooleanExpression primaryCondition = ascending
                ? article.publishedAt.gt(cursor)
                : article.publishedAt.lt(cursor);

        BooleanExpression createdAtTieBreaker = article.publishedAt.eq(cursor)
                .and(ascending
                        ? article.createdAt.gt(after)
                        : article.createdAt.lt(after));

        BooleanExpression idTieBreaker = article.publishedAt.eq(cursor)
                .and(article.createdAt.eq(after))
                .and(ascending
                        ? article.id.gt(articleId)
                        : article.id.lt(articleId));

        return primaryCondition
                .or(createdAtTieBreaker)
                .or(idTieBreaker);
    }

    private BooleanExpression commentCountCursorCondition(
            Long cursor,
            LocalDateTime after,
            UUID articleId,
            boolean ascending
    ) {
        BooleanExpression primaryCondition = ascending
                ? article.commentCount.gt(cursor)
                : article.commentCount.lt(cursor);

        BooleanExpression createdAtTieBreaker = article.commentCount.eq(cursor)
                .and(ascending
                        ? article.createdAt.gt(after)
                        : article.createdAt.lt(after));

        BooleanExpression idTieBreaker = article.commentCount.eq(cursor)
                .and(article.createdAt.eq(after))
                .and(ascending
                        ? article.id.gt(articleId)
                        : article.id.lt(articleId));

        return primaryCondition
                .or(createdAtTieBreaker)
                .or(idTieBreaker);
    }

    private BooleanExpression viewCountCursorCondition(
            Long cursor,
            LocalDateTime after,
            UUID articleId,
            boolean ascending
    ) {
        BooleanExpression primaryCondition = ascending
                ? article.viewCount.gt(cursor)
                : article.viewCount.lt(cursor);

        BooleanExpression createdAtTieBreaker = article.viewCount.eq(cursor)
                .and(ascending
                        ? article.createdAt.gt(after)
                        : article.createdAt.lt(after));

        BooleanExpression idTieBreaker = article.viewCount.eq(cursor)
                .and(article.createdAt.eq(after))
                .and(ascending
                        ? article.id.gt(articleId)
                        : article.id.lt(articleId));

        return primaryCondition
                .or(createdAtTieBreaker)
                .or(idTieBreaker);
    }

    private String getNextCursor(List<Article> content, String orderBy, boolean hasNext) {
        if (!hasNext || content.isEmpty()) {
            return null;
        }

        Article lastArticle = content.get(content.size() - 1);

        return switch (orderBy) {
            case "publishDate" -> lastArticle.getPublishedAt() + "|" + lastArticle.getId();
            case "commentCount" -> lastArticle.getCommentCount() + "|" + lastArticle.getId();
            case "viewCount" -> lastArticle.getViewCount() + "|" + lastArticle.getId();
            default -> throw invalidSearchCondition("orderBy", orderBy);
        };
    }

    private String getNextAfter(List<Article> content, boolean hasNext) {
        if (!hasNext || content.isEmpty()) {
            return null;
        }

        return content.get(content.size() - 1)
                .getCreatedAt()
                .toString();
    }
}