package com.codeit.monew.domain.comment.repository;

import com.codeit.monew.domain.comment.entity.Comment;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CommentRepository extends JpaRepository<Comment, UUID> {

  @Query("""
      SELECT c FROM Comment c
      JOIN FETCH c.user
      WHERE c.article.id = :articleId
      ORDER BY c.createdAt DESC, c.id DESC 
      LIMIT :limit
      """)
  List<Comment> findByArticleIdFirstPage (@Param("articleId") UUID articleId, @Param("limit") int limit);

  @Query("""
      SELECT c FROM Comment c
      JOIN FETCH c.user
      WHERE c.article.id = :articleId
        AND (c.createdAt < :after
          OR (c.createdAt = : after AND c.id < :cusorId))
      ORDER BY c.createdAt DESC, c.id DESC 
      LIMIT :limit
      """)
  List<Comment> findByArticleIdAfterCursor (
      @Param("articleId") UUID articleId,
      @Param("after")LocalDateTime after,
      @Param("cursorId") UUID cursorId,
      @Param("limit") int limit
  );

  long countByArticleId(UUID articleId);
}
