package com.codeit.monew.domain.comment.repository;

import com.codeit.monew.domain.comment.entity.Comment;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


public interface CommentRepository extends JpaRepository<Comment, UUID>, CommentRepositoryCustom {

  long countByArticleId(UUID articleId);

  @Query(value = """
      SELECT id FROM comments
      WHERE is_deleted = true
        AND deleted_at < :threshold
      ORDER BY deleted_at ASC
      LIMIT :limit
      """, nativeQuery = true)
  List<UUID> findIdsByDeletedAtBefore(@Param("threshold") LocalDateTime threshold, @Param("limit") int limit);

  @Modifying(clearAutomatically = true)
  @Query(value = "DELETE FROM comments WHERE id IN (:ids)", nativeQuery = true)
  void hardDeleteAllByIdIn(@Param("ids") List<UUID> ids);
}