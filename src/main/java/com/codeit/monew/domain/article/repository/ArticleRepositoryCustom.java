package com.codeit.monew.domain.article.repository;

import com.codeit.monew.domain.article.dto.request.ArticleSearchRequest;
import com.codeit.monew.domain.article.entity.Article;
import java.util.List;

public interface ArticleRepositoryCustom {

    List<Article> searchArticles(ArticleSearchRequest request);
}
