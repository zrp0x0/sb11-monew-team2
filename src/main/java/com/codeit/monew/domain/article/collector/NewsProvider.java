package com.codeit.monew.domain.article.collector;

import com.codeit.monew.domain.article.entity.ArticleSource;
import java.util.List;

public interface NewsProvider {

    ArticleSource source();

    List<CollectedArticle> collect();
}