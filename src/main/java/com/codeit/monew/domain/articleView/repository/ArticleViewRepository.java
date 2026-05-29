package com.codeit.monew.domain.articleView.repository;

import com.codeit.monew.domain.articleView.entity.ArticleView;
import java.util.Optional;
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
}
