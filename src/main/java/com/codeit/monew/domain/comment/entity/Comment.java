package com.codeit.monew.domain.comment.entity;

import com.codeit.monew.domain.article.entity.Article;
import com.codeit.monew.domain.user.entity.User;
import com.codeit.monew.global.entity.BaseSoftDeleteEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import static com.codeit.monew.global.entity.BaseSoftDeleteEntity.*;

@Entity
@Table(name = "comments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
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
