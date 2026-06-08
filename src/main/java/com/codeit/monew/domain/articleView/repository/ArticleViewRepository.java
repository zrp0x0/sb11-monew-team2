package com.codeit.monew.domain.articleView.repository;

import com.codeit.monew.domain.articleView.entity.ArticleView;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ArticleViewRepository extends JpaRepository<ArticleView, UUID> {

  @Query("""
      SELECT av
      FROM ArticleView av
      WHERE av.user.id = :userId
        AND av.article.id = :articleId
      """)
  Optional<ArticleView> findByUserIdAndArticleId(
      @Param("userId") UUID userId,
      @Param("articleId") UUID articleId
  );

  @Query("SELECT v.article.id FROM ArticleView v WHERE v.user.id = :userId AND v.article.id IN :articleIds")
  Set<UUID> findViewedArticleIds(
      @Param("userId") UUID userId,
      @Param("articleIds") List<UUID> articleIds
  );
}
