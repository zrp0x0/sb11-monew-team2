package com.codeit.monew.domain.comment.entity;

import com.codeit.monew.domain.article.entity.Article;
import com.codeit.monew.domain.user.entity.User;
import com.codeit.monew.global.entity.BaseSoftDeleteEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import java.util.UUID;

import static com.codeit.monew.global.entity.BaseSoftDeleteEntity.IS_DELETED_FALSE;

@Entity
@Table(name = "comments", indexes = {
    @Index(name = "idx_comments_deleted_at", columnList = "deleted_at")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLRestriction(IS_DELETED_FALSE)
public class Comment extends BaseSoftDeleteEntity{

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "article_id", nullable = false)
  private Article article;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Column(name = "content", nullable = false, length = 500)
  private String content;

  @Column(name = "like_counts", nullable = false, columnDefinition = "INT DEFAULT 0")
  private int likeCounts = 0;

  private Comment(Article article, User user, String content) {
    this.article = article;
    this.user = user;
    this.content = content;
  }

  public static Comment create(Article article, User user, String content) {
    return new Comment(article, user, content);
  }

  public void update(String content) {
    this.content = content;
  }

  public void increaseLikeCount() {
    this.likeCounts++;
  }

  public void decreaseLikeCount() {
    if(this.likeCounts > 0) {
      this.likeCounts--;
    }
  }
}
