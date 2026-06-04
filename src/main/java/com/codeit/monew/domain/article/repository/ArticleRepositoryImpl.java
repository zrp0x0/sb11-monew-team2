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

    @Override
    public CursorPageResponse<Article> searchArticles(ArticleSearchRequest request) {

        String orderBy =
            StringUtils.hasText(request.orderBy()) ? request.orderBy().trim() : "publishDate";
        String direction =
            StringUtils.hasText(request.direction()) ? request.direction().trim() : "DESC";

        // 1. 목록을 조회함
        List<Article> articles = queryFactory
            .selectFrom(article)
            .where(
                isNotDeleted(),
                searchInterestId(request.interestId()), // 관심사 필터링 처리함
                searchKeyword(request.keyword()),
                searchSourceIn(request.sourceIn()),
                searchPublishDate(request.publishDateFrom(), request.publishDateTo()),
                cursorCondition(orderBy, direction, request.cursor(), request.after())
            )
            .orderBy(createOrderSpecifier(orderBy, direction))
            .limit(request.limit() + 1)
            .fetch();

        // 2. 다음 페이지 존재 여부를 확인하고 리스트 사이즈를 조정함
        boolean hasNext = articles.size() > request.limit();
        String nextCursor = null;
        LocalDateTime nextAfter = null;

        if (hasNext) {
            articles.remove(articles.size() - 1);
        }

        if (hasNext && !articles.isEmpty()) {
            Article lastArticle = articles.get(articles.size() - 1);
            nextAfter = lastArticle.getCreatedAt();

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(
                "yyyy-MM-dd'T'HH:mm:ss.SSSSSS");

            if ("publishDate".equals(orderBy)) {
                String formattedDate = lastArticle.getPublishedAt().format(formatter);
                nextCursor = formattedDate + "_" + lastArticle.getId();
            } else if ("commentCount".equals(orderBy)) {
                nextCursor = lastArticle.getCommentCount() + "_" + lastArticle.getId();
            } else {
                nextCursor = lastArticle.getViewCount() + "_" + lastArticle.getId();
            }
        }

        // 4. 전체 카운트를 조회함 (첫 페이지 조회 시에만 실행하여 성능 chlwjrghk
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
            nextAfter != null ? nextAfter.toString() : null,
            request.limit(),
            totalElementCount,
            hasNext
        );
    }

    private BooleanExpression isNotDeleted() {
        return article.deletedAt.isNull();
    }

    private BooleanExpression searchInterestId(UUID interestId) {
        if (interestId == null) {
            return null;
        }
        // EXISTS 서브쿼리를 적용하여 조인으로 인한 데이터 중복을 방지함
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
        // 1. 진짜 첫 페이지 요청
        if (!StringUtils.hasText(cursor) || "null".equalsIgnoreCase(cursor.trim())
            || "undefined".equalsIgnoreCase(cursor.trim())) {
            return null;
        }

        // 2. 형식이 완전히 깨진 커서 방어
        if (!cursor.contains("_")) {
            return Expressions.asBoolean(true).isFalse();
        }

        boolean isAsc = "ASC".equalsIgnoreCase(direction);
        int lastDashIndex = cursor.lastIndexOf("_");

        try {
            String primaryCursorValue = cursor.substring(0, lastDashIndex);
            UUID cursorId = UUID.fromString(cursor.substring(lastDashIndex + 1));

            if ("publishDate".equals(orderBy)) {
                LocalDateTime cursorDate = LocalDateTime.parse(primaryCursorValue);
                if (isAsc) {
                    if (after != null) {
                        return article.publishedAt.gt(cursorDate)
                            .or(article.publishedAt.eq(cursorDate).and(article.createdAt.gt(after)))
                            .or(article.publishedAt.eq(cursorDate).and(article.createdAt.eq(after))
                                .and(article.id.gt(cursorId)));
                    } else {
                        // 프론트엔드가 after를 빼먹었을 때를 대비한 안전망 (createdAt 조건 무시하고 ID로만 비교함)
                        return article.publishedAt.gt(cursorDate)
                            .or(article.publishedAt.eq(cursorDate).and(article.id.gt(cursorId)));
                    }
                } else {
                    if (after != null) {
                        return article.publishedAt.lt(cursorDate)
                            .or(article.publishedAt.eq(cursorDate).and(article.createdAt.lt(after)))
                            .or(article.publishedAt.eq(cursorDate).and(article.createdAt.eq(after))
                                .and(article.id.lt(cursorId)));
                    } else {
                        return article.publishedAt.lt(cursorDate)
                            .or(article.publishedAt.eq(cursorDate).and(article.id.lt(cursorId)));
                    }
                }
            } else if ("commentCount".equals(orderBy)) {
                Long cursorCount = Long.valueOf(primaryCursorValue);
                if (isAsc) {
                    if (after != null) {
                        return article.commentCount.gt(cursorCount)
                            .or(article.commentCount.eq(cursorCount)
                                .and(article.createdAt.gt(after)))
                            .or(article.commentCount.eq(cursorCount)
                                .and(article.createdAt.eq(after)).and(article.id.gt(cursorId)));
                    } else {
                        return article.commentCount.gt(cursorCount)
                            .or(article.commentCount.eq(cursorCount).and(article.id.gt(cursorId)));
                    }
                } else {
                    if (after != null) {
                        return article.commentCount.lt(cursorCount)
                            .or(article.commentCount.eq(cursorCount)
                                .and(article.createdAt.lt(after)))
                            .or(article.commentCount.eq(cursorCount)
                                .and(article.createdAt.eq(after)).and(article.id.lt(cursorId)));
                    } else {
                        return article.commentCount.lt(cursorCount)
                            .or(article.commentCount.eq(cursorCount).and(article.id.lt(cursorId)));
                    }
                }
            } else {
                Long cursorCount = Long.valueOf(primaryCursorValue);
                if (isAsc) {
                    if (after != null) {
                        return article.viewCount.gt(cursorCount)
                            .or(article.viewCount.eq(cursorCount).and(article.createdAt.gt(after)))
                            .or(article.viewCount.eq(cursorCount).and(article.createdAt.eq(after))
                                .and(article.id.gt(cursorId)));
                    } else {
                        return article.viewCount.gt(cursorCount)
                            .or(article.viewCount.eq(cursorCount).and(article.id.gt(cursorId)));
                    }
                } else {
                    if (after != null) {
                        return article.viewCount.lt(cursorCount)
                            .or(article.viewCount.eq(cursorCount).and(article.createdAt.lt(after)))
                            .or(article.viewCount.eq(cursorCount).and(article.createdAt.eq(after))
                                .and(article.id.lt(cursorId)));
                    } else {
                        return article.viewCount.lt(cursorCount)
                            .or(article.viewCount.eq(cursorCount).and(article.id.lt(cursorId)));
                    }
                }
            }
        } catch (Exception e) {
            // 날짜 파싱 에러나 UUID 파싱 에러가 발생해도 서버가 터지지 않고 0건을 반환하여 스크롤을 얌전하게 종료시킴
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
            // viewCount 정렬 처리함
            return new OrderSpecifier[]{
                new OrderSpecifier<>(order, article.viewCount),
                new OrderSpecifier<>(order, article.createdAt),
                new OrderSpecifier<>(order, article.id)
            };
        }
    }
}