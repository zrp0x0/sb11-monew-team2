package com.codeit.monew.domain.subscription.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.util.UUID;

@Entity
public class Subscription {

  @Id
  private UUID id;
}
