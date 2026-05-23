package com.codeit.monew.domain.interest.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.util.UUID;

@Entity
public class Interest {

  @Id
  private UUID id;
}
