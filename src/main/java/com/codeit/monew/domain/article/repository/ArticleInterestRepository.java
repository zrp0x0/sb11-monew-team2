package com.codeit.monew.domain.article.repository;

import com.codeit.monew.domain.article.entity.ArticleInterest;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArticleInterestRepository extends JpaRepository<ArticleInterest, UUID> {

    List<ArticleInterest> findByArticleIdIn(List<UUID> articleIds);
}