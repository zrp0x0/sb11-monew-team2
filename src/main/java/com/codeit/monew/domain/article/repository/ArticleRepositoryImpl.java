package com.codeit.monew.domain.article.repository;

import static com.codeit.monew.domain.article.entity.QArticle.article;

import com.codeit.monew.domain.article.dto.request.ArticleSearchRequest;
import com.codeit.monew.domain.article.entity.Article;
import com.codeit.monew.domain.article.entity.ArticleSource;
import com.codeit.monew.global.dto.CursorPageResponse;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.LocalDateTime;
import java.util.List;
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

    private void validateLimit(int limit) {
        if (limit <= 0) {
            throw new IllegalArgumentException("limit은 1 이상이어야 합니다.");
        }
    }

    private Long countTotalElements(ArticleSearchRequest request) {
        Long count = queryFactory
                .select(article.count())
                .from(article)
                .where(
                        notDeleted(),
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
                    new OrderSpecifier<>(order, article.createdAt)
            };
            case "commentCount" -> new OrderSpecifier<?>[] {
                    new OrderSpecifier<>(order, article.commentCount),
                    new OrderSpecifier<>(order, article.createdAt)
            };
            case "viewCount" -> new OrderSpecifier<?>[] {
                    new OrderSpecifier<>(order, article.viewCount),
                    new OrderSpecifier<>(order, article.createdAt)
            };
            default -> throw new IllegalArgumentException("지원하지 않는 정렬 기준입니다.");
        };
    }

    private boolean isAscending(String direction) {
        if ("ASC".equalsIgnoreCase(direction)) {
            return true;
        }

        if ("DESC".equalsIgnoreCase(direction)) {
            return false;
        }

        throw new IllegalArgumentException("지원하지 않는 정렬 방향입니다.");
    }

    private BooleanExpression cursorCondition(ArticleSearchRequest request) {
        if (!StringUtils.hasText(request.cursor())) {
            return null;
        }

        boolean ascending = isAscending(request.direction());

        return switch (request.orderBy()) {
            case "publishDate" -> publishDateCursorCondition(
                    LocalDateTime.parse(request.cursor()),
                    request.after(),
                    ascending
            );
            case "commentCount" -> commentCountCursorCondition(
                    Long.valueOf(request.cursor()),
                    request.after(),
                    ascending
            );
            case "viewCount" -> viewCountCursorCondition(
                    Long.valueOf(request.cursor()),
                    request.after(),
                    ascending
            );
            default -> throw new IllegalArgumentException("지원하지 않는 정렬 기준입니다.");
        };
    }

    private BooleanExpression publishDateCursorCondition(
            LocalDateTime cursor,
            LocalDateTime after,
            boolean ascending
    ) {
        BooleanExpression primaryCondition = ascending
                ? article.publishedAt.gt(cursor)
                : article.publishedAt.lt(cursor);

        if (after == null) {
            return primaryCondition;
        }

        BooleanExpression tieBreakerCondition = article.publishedAt.eq(cursor)
                .and(ascending
                        ? article.createdAt.gt(after)
                        : article.createdAt.lt(after));

        return primaryCondition.or(tieBreakerCondition);
    }

    private BooleanExpression commentCountCursorCondition(
            Long cursor,
            LocalDateTime after,
            boolean ascending
    ) {
        BooleanExpression primaryCondition = ascending
                ? article.commentCount.gt(cursor)
                : article.commentCount.lt(cursor);

        if (after == null) {
            return primaryCondition;
        }

        BooleanExpression tieBreakerCondition = article.commentCount.eq(cursor)
                .and(ascending
                        ? article.createdAt.gt(after)
                        : article.createdAt.lt(after));

        return primaryCondition.or(tieBreakerCondition);
    }

    private BooleanExpression viewCountCursorCondition(
            Long cursor,
            LocalDateTime after,
            boolean ascending
    ) {
        BooleanExpression primaryCondition = ascending
                ? article.viewCount.gt(cursor)
                : article.viewCount.lt(cursor);

        if (after == null) {
            return primaryCondition;
        }

        BooleanExpression tieBreakerCondition = article.viewCount.eq(cursor)
                .and(ascending
                        ? article.createdAt.gt(after)
                        : article.createdAt.lt(after));

        return primaryCondition.or(tieBreakerCondition);
    }

    private String getNextCursor(List<Article> content, String orderBy, boolean hasNext) {
        if (!hasNext || content.isEmpty()) {
            return null;
        }

        Article lastArticle = content.get(content.size() - 1);

        return switch (orderBy) {
            case "publishDate" -> lastArticle.getPublishedAt().toString();
            case "commentCount" -> String.valueOf(lastArticle.getCommentCount());
            case "viewCount" -> String.valueOf(lastArticle.getViewCount());
            default -> throw new IllegalArgumentException("지원하지 않는 정렬 기준입니다.");
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