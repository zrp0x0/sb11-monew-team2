package com.codeit.monew.domain.interest.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.util.UUID;

@Entity
public class interest {

  @Id
  private UUID id;
}
