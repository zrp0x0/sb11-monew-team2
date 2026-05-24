package com.codeit.monew.domain.article.entity;

import com.codeit.monew.global.entity.BaseSoftDeleteEntity;
import com.codeit.monew.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import static com.codeit.monew.global.entity.BaseSoftDeleteEntity.*;

@Entity
@Table(
        name = "articles",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_articles_source_url", columnNames = "source_url")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLRestriction(IS_DELETED_FALSE)
public class Article extends BaseSoftDeleteEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(columnDefinition = "uuid", nullable = false, updatable = false)
  private UUID id;

  @Enumerated(EnumType.STRING)
  @Column(name = "source", nullable = false, length = 30)
  private ArticleSource source;

  @Column(name = "source_url", nullable = false, length = 2048)
  private String sourceUrl;

  @Column(name = "title", nullable = false, length = 500)
  private String title;

  @Column(name = "summary", nullable = false, length = 2000)
  private String summary;

  @Column(name = "published_at", nullable = false)
  private LocalDateTime publishedAt;

  @Column(name = "view_count", nullable = false)
  private long viewCount = 0L;

  @Column(name = "comment_count", nullable = false)
  private long commentCount = 0L;

  private Article(
          ArticleSource source,
          String sourceUrl,
          String title,
          String summary,
          LocalDateTime publishedAt
  ) {
    this.source = source;
    this.sourceUrl = sourceUrl;
    this.title = title;
    this.summary = summary;
    this.publishedAt = publishedAt;
  }

  public static Article create(
          ArticleSource source,
          String sourceUrl,
          String title,
          String summary,
          LocalDateTime publishedAt
  ) {
    return new Article(source, sourceUrl, title, summary, publishedAt);
  }
}