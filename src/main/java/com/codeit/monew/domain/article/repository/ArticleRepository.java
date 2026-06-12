package com.codeit.monew.domain.article.repository;

import com.codeit.monew.domain.article.entity.Article;

import java.time.LocalDateTime;
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
               WHERE source_url = :sourceUrl)
          """, nativeQuery = true
  )
  boolean existsBySourceUrlIncludingDeleted(@Param("sourceUrl") String sourceUrl);

  @Modifying(clearAutomatically = true)
  @Query(value = """
      INSERT INTO articles (id, source, source_url, title, summary, published_at, view_count, comment_count, is_deleted, created_at, updated_at)
      VALUES (:#{#a.id}, :#{#a.source.name()}, :#{#a.sourceUrl}, :#{#a.title}, :#{#a.summary}, :#{#a.publishedAt}, :#{#a.viewCount}, :#{#a.commentCount}, false, :#{#a.createdAt}, now())
      ON CONFLICT (source_url) DO NOTHING
      """, nativeQuery = true)
  int upsertArticleSkipDuplicate(@Param("a") Article article);

  // == 기사 물리 삭제용 ==
  // 지워야 할 옛날 기사의 ID를 Limit 개수만큼만 조회
  @Query(value = "SELECT id FROM articles WHERE created_at < :cutoffDate LIMIT :limit", nativeQuery = true)
  List<UUID> findOldArticleIdsWithLimit(@Param("cutoffDate") LocalDateTime cutoffDate, @Param("limit") int limit);

  // 댓글 좋아요 삭제 (CommentLike)
  @Modifying(clearAutomatically = true)
  @Query(value = "DELETE FROM comment_likes WHERE comment_id IN (SELECT id FROM comments WHERE article_id IN :articleIds)", nativeQuery = true)
  void hardDeleteCommentLikesByArticleIds(@Param("articleIds") List<UUID> articleIds);

  // 댓글 삭제 (Comment)
  @Modifying(clearAutomatically = true)
  @Query(value = "DELETE FROM comments WHERE article_id IN :articleIds", nativeQuery = true)
  void hardDeleteCommentsByArticleIds(@Param("articleIds") List<UUID> articleIds);

  // 기사 조회 기록 삭제 (ArticleView)
  @Modifying(clearAutomatically = true)
  @Query(value = "DELETE FROM article_views WHERE article_id IN :articleIds", nativeQuery = true)
  void hardDeleteArticleViewsByArticleIds(@Param("articleIds") List<UUID> articleIds);

  // 기사 관심사 매핑 삭제 (ArticleInterest)
  @Modifying(clearAutomatically = true)
  @Query(value = "DELETE FROM article_interests WHERE article_id IN :articleIds", nativeQuery = true)
  void hardDeleteArticleInterestsByArticleIds(@Param("articleIds") List<UUID> articleIds);

  // 최종 기사 삭제 (Article)
  @Modifying(clearAutomatically = true)
  @Query(value = "DELETE FROM articles WHERE id IN :articleIds", nativeQuery = true)
  void hardDeleteArticlesByIds(@Param("articleIds") List<UUID> articleIds);
}
