package com.codeit.monew.domain.article.repository;

import com.codeit.monew.domain.article.entity.Article;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ArticleRepository extends JpaRepository<Article, UUID>, ArticleRepositoryCustom {

  Optional<Article> findByIdAndDeletedAtIsNull(UUID articleId);

  boolean existsBySourceUrl(String sourceUrl);
    Optional<Article> findByIdAndDeletedAtIsNull(UUID articleId);

    List<Article> findBySourceUrlIn(List<String> sourceUrls);

    @Query(
            value = """
                    SELECT EXISTS (
                         SELECT 1
                         FROM articles
                         WHERE source_url = :sourceUrl                                          
                    )
                    """,
            nativeQuery = true
    )

    boolean existsBySourceUrlIncludingDeleted(@Param("sourceUrl") String sourceUrl);
}