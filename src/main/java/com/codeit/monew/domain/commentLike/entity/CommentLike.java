package com.codeit.monew.domain.commentLike.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.util.UUID;

@Entity
public class CommentLike {

  @Id
  private UUID id;
}
