package com.codeit.monew.domain.articleView.repository;

import com.codeit.monew.domain.articleView.entity.ArticleView;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArticleViewRepository extends JpaRepository<ArticleView, UUID> {

    Optional<ArticleView> findByUserIdAndArticleId(UUID userId, UUID articleId);
}
