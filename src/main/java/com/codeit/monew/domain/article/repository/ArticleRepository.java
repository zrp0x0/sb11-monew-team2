package com.codeit.monew.domain.article.repository;

import com.codeit.monew.domain.article.entity.Article;
import com.codeit.monew.domain.article.entity.ArticleSource;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ArticleRepository extends JpaRepository<Article, UUID> {

    @Query("""
      select distinct a.source
      from Article a
      where a.deletedAt is null
      order by a.source
      """)
    List<ArticleSource> findDistinctSources();
}