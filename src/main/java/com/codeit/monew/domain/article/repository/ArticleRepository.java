package com.codeit.monew.domain.article.repository;

import com.codeit.monew.domain.article.entity.Article;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ArticleRepository extends JpaRepository<Article, UUID>, ArticleRepositoryCustom {

    Optional<Article> findByIdAndDeletedAtIsNull(UUID articleId);

    @Query("select a.sourceUrl from Article a where a.sourceUrl in :sourceUrls")
    Set<String> findExistingSourceUrls(@Param("sourceUrls") Collection<String> sourceUrls);
}