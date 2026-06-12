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

  // [복구용] 매핑 테이블 복구 (새로운 기사와 관심사를 연결)
  // 이미 존재하는 매핑이거나 관심사가 DB에 존재하지 않으면 에러 없이 무시
  @Modifying(clearAutomatically = true)
  @Query(value = """
      INSERT INTO article_interests (id, article_id, interest_id)
      SELECT :id, :articleId, :interestId
      WHERE NOT EXISTS (
          SELECT 1 FROM article_interests WHERE article_id = :articleId AND interest_id = :interestId
      )
      AND EXISTS (
          SELECT 1 FROM interests WHERE id = :interestId
      )
      """, nativeQuery = true)
  int insertIgnoreMapping(@Param("id") UUID id, @Param("articleId") UUID articleId, @Param("interestId") UUID interestId);
}