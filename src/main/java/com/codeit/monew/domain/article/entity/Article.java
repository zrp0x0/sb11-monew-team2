package com.codeit.monew.domain.article.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.util.UUID;

@Entity
public class Article {

  @Id
  private UUID id;
}
