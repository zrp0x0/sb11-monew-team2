package com.codeit.monew.batch.collector.provider;

import com.codeit.monew.domain.article.entity.ArticleSource;
import com.codeit.monew.domain.interest.entity.Interest;

public interface NewsProvider {

    NewsFetchResult fetchNews(Interest interest);

    ArticleSource getSource();
}
