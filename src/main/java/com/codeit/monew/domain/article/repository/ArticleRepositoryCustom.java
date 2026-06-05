package com.codeit.monew.domain.article.repository;

import com.codeit.monew.domain.article.dto.request.ArticleSearchRequest;
import com.codeit.monew.domain.article.dto.request.CursorPageResponseDate;
import com.codeit.monew.domain.article.entity.Article;

public interface ArticleRepositoryCustom {

    CursorPageResponseDate<Article> searchArticles(ArticleSearchRequest request);
}