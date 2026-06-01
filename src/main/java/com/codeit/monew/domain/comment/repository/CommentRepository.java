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

  @Query("SELECT c.id FROM Comment c WHERE c.deletedAt < :threshold AND c.isDeleted = true ORDER BY c.deletedAt ASC LIMIT :limit")
  List<UUID> findIdsByDeletedAtBefore(@Param("threshold") LocalDateTime threshold, @Param("limit") int limit);
}
