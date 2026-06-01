package com.codeit.monew.domain.commentLike.repository;

import com.codeit.monew.domain.commentLike.entity.CommentLike;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CommentLikeRepository extends JpaRepository<CommentLike, UUID> {
    Optional<CommentLike> findByCommentIdAndUserId(UUID commentId, UUID userId);

    @Query("SELECT cl.comment.id FROM CommentLike cl WHERE cl.user.id = :userId AND cl.comment.id IN :commentIds")
    List<UUID> findByUserIdAndCommentIdIn(@Param("userId") UUID userId, @Param("commentIds") List<UUID> commentIds);

    void deleteAllByCommentId(UUID commentId);

    void deleteAllByCommentIdIn(List<UUID> commentIds);
}
