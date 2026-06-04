package com.codeit.monew.domain.article.entity;

import com.codeit.monew.domain.interest.entity.Interest;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "article_interests",
    uniqueConstraints = {
        // 동일한 기사가 동일한 관심사에 중복 매핑되는 것을 방지
        @UniqueConstraint(name = "uk_article_interest", columnNames = {"article_id", "interest_id"})
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ArticleInterest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "article_id", nullable = false)
    private Article article;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "interest_id", nullable = false)
    private Interest interest;

    private ArticleInterest(Article article, Interest interest) {
        this.article = article;
        this.interest = interest;
    }

    public static ArticleInterest create(Article article, Interest interest) {
        return new ArticleInterest(article, interest);
    }
}