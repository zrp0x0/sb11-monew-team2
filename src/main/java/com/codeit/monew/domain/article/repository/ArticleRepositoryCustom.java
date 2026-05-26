package com.codeit.monew.domain.article.repository;

import com.codeit.monew.domain.article.dto.request.ArticleSearchRequest;
import com.codeit.monew.domain.article.entity.Article;
import com.codeit.monew.global.dto.CursorPageResponse;

public interface ArticleRepositoryCustom {

    CursorPageResponse<Article> searchArticles(ArticleSearchRequest request);
}