package com.codeit.monew.domain.article.repository;

import com.codeit.monew.domain.article.entity.ArticleInterest;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ArticleInterestRepository extends JpaRepository<ArticleInterest, UUID> {

  List<ArticleInterest> findByArticleIdIn(List<UUID> articleIds);

  void deleteByInterestId(UUID interestId);

  // [백업용] 특정 기사에 매핑된 관심사 ID 목록 조회
  @Query("SELECT ai.interest.id FROM ArticleInterest ai WHERE ai.article.id = :articleId")
  List<UUID> findInterestIdsByArticleId(@Param("articleId") UUID articleId);

  // [복구용] 매핑 테이블 복구 (이미 연결되어 있으면 무시 - ON CONFLICT DO NOTHING)
  @Modifying(clearAutomatically = true)
  @Query(value = """
      INSERT INTO article_interests (id, article_id, interest_id)
      VALUES (:id, :articleId, :interestId)
      ON CONFLICT (article_id, interest_id) DO NOTHING
      """, nativeQuery = true)
  void insertIgnoreMapping(@Param("id") UUID id, @Param("articleId") UUID articleId, @Param("interestId") UUID interestId);
}