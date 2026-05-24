package com.codeit.monew.domain.subscription.entity;

import com.codeit.monew.domain.interest.entity.Interest;
import com.codeit.monew.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "subscriptions", indexes = {
        @Index(
                name = "idx_subscription_interest_user",
                columnList = "interest_id, user_id,",
                unique = true
        )
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Subscription {

  @Id @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "interest_id", nullable = false)
  private Interest interest;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @CreatedDate
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  private Subscription(Interest interest, User user) {
    this.interest = interest;
    this.user = user;
  }

  public static Subscription create(Interest interest, User user) {
    return new Subscription(interest, user);
  }
}
