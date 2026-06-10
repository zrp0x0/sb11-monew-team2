package com.codeit.monew.domain.article.controller;

import com.codeit.monew.domain.article.dto.request.ArticleSearchRequest;
import com.codeit.monew.domain.article.entity.ArticleSource;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ArticleSearchRequestNormalizer {

    public ArticleSearchRequest normalize(
            ArticleSearchRequest request,
            List<ArticleSource> sourceInBrackets
    ) {
        List<ArticleSource> normalizedSourceIn = new ArrayList<>();

        if (request.sourceIn() != null && !request.sourceIn().isEmpty()) {
            normalizedSourceIn.addAll(request.sourceIn());
        }

        if (sourceInBrackets != null && !sourceInBrackets.isEmpty()) {
            normalizedSourceIn.addAll(sourceInBrackets);
        }

        if (normalizedSourceIn.isEmpty()) {
            return request;
        }

        List<ArticleSource> distinctSourceIn = new ArrayList<>(
                new LinkedHashSet<>(normalizedSourceIn)
        );

        return new ArticleSearchRequest(
                request.keyword(),
                request.interestId(),
                distinctSourceIn,
                request.publishDateFrom(),
                request.publishDateTo(),
                request.orderBy(),
                request.direction(),
                request.cursor(),
                request.after(),
                request.limit(),
                request.requestUserId()
        );
    }
}