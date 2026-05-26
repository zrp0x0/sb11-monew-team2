package com.codeit.monew.domain.interest.repository;

import com.codeit.monew.domain.interest.entity.Interest;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InterestRepository extends JpaRepository<Interest, UUID> {

}
