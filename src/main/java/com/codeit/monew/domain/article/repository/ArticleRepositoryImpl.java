package com.codeit.monew.domain.article.repository;

import static com.codeit.monew.domain.article.entity.QArticle.article;

import com.codeit.monew.domain.article.dto.request.ArticleSearchRequest;
import com.codeit.monew.domain.article.entity.Article;
import com.codeit.monew.domain.article.entity.ArticleSource;
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
    public List<Article> searchArticles(ArticleSearchRequest request) {
        return queryFactory
                .selectFrom(article)
                .where(
                        notDeleted(),
                        keywordContains(request.keyword()),
                        sourceIn(request.sourceIn()),
                        publishedAtGoe(request.publishDateFrom()),
                        publishedAtLoe(request.publishDateTo())
                )
                .fetch();
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
}