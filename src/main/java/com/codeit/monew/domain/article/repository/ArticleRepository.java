package com.codeit.monew.domain.article.repository;

import com.codeit.monew.domain.article.entity.Article;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ArticleRepository extends JpaRepository<Article, UUID>, ArticleRepositoryCustom {

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

  @Modifying(clearAutomatically = true)
  @Query(value = """
      INSERT INTO articles (id, source, source_url, title, summary, published_at, view_count, comment_count, is_deleted, created_at, updated_at) 
      VALUES (:#{#a.id}, :#{#a.source.name()}, :#{#a.sourceUrl}, :#{#a.title}, :#{#a.summary}, :#{#a.publishedAt}, :#{#a.viewCount}, :#{#a.commentCount}, false, now(), now()) 
      ON CONFLICT (source_url) DO NOTHING
      """, nativeQuery = true)
  int upsertArticleSkipDuplicate(@Param("a") Article article);
}
