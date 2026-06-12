package com.codeit.monew.domain.interest.repository;

import com.codeit.monew.domain.interest.dto.request.InterestSearchRequest;
import com.codeit.monew.domain.interest.entity.Interest;
import java.util.List;

public interface InterestRepositoryCustom {

  List<Interest> findAllByCondition(InterestSearchRequest request);
}
