package com.codeit.monew.domain.commentLike.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.util.UUID;

@Entity
public class commentLike {

  @Id
  private UUID id;
}
