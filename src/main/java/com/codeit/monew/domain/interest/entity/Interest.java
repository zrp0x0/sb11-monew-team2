package com.codeit.monew.domain.interest.entity;

import com.codeit.monew.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "interests")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Interest extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(nullable = false, unique = true, length = 50, updatable = false)
  private String name;

  @Column(name = "subscriber_count", nullable = false)
  private Long subscriberCount;

  @ElementCollection(fetch = FetchType.LAZY)
  @CollectionTable(
      name = "interest_keyword",
      joinColumns = @JoinColumn(name = "interest_id")
  )
  @Column(name = "keyword", nullable = false)
  private List<String> keywords = new ArrayList<>();

  private Interest(String name, List<String> keywords) {
    this.name = name;
    this.subscriberCount = 0L;
    if (keywords != null) {
      this.keywords.addAll(keywords);
    }
  }

  public static Interest create(String name, List<String> keywords) {
    return new Interest(name, keywords);
  }

  public void increaseSubscriberCount() {
    this.subscriberCount++;
  }

  public void decreaseSubscriberCount() {
    if (this.subscriberCount > 0) {
      this.subscriberCount--;
    }
  }

  public void updateKeywords(List<String> newKeywords) {
    this.keywords.clear();
    if (newKeywords != null) {
      this.keywords.addAll(newKeywords);
    }
  }
}