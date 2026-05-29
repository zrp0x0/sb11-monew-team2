package com.codeit.monew.domain.interest.repository;

import static com.codeit.monew.domain.interest.entity.QInterest.interest;

import com.codeit.monew.domain.interest.dto.request.InterestSearchRequest;
import com.codeit.monew.domain.interest.entity.Interest;
import com.codeit.monew.domain.interest.exception.InterestErrorCode;
import com.codeit.monew.domain.interest.exception.InterestException;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class InterestRepositoryImpl implements InterestRepositoryCustom {

  private final JPAQueryFactory jpaQueryFactory;

  @Override
  public List<Interest> findAllByCondition(InterestSearchRequest request) {
    return jpaQueryFactory
        .selectFrom(interest)
        .where(
            containsKeyword(request.keyword()),
            cursorCondition(request)
        )
        .orderBy(
            dynamicOrder(request),
            interest.createdAt.desc(), // 동점자 발생 시 최신순으로 정렬
            interest.id.asc() // 생성일시까지 같을 경우 ID순으로 정렬
        )
        .limit(request.getLimit() + 1)
        .fetch();
  }

  private BooleanExpression containsKeyword(String keyword) {
    if (keyword == null || keyword.isBlank()) {
      return null;
    }

    // 대소문자를 무시하고 관심사 또는 키워드에 포함되는지 확인
    return interest.name.containsIgnoreCase(keyword)
        .or(interest.keywords.any().containsIgnoreCase(keyword));
  }

  private OrderSpecifier<?> dynamicOrder(InterestSearchRequest request) {
    boolean isDesc = request.getDirection().equalsIgnoreCase("DESC");

    if (request.getOrderBy().equals("name")) {
      return isDesc ? interest.name.desc() : interest.name.asc();
    }

    return isDesc ? interest.subscriberCount.desc() : interest.subscriberCount.asc();
  }

  private BooleanExpression cursorCondition(InterestSearchRequest request) {
    String cursor = request.cursor();
    LocalDateTime after = request.after();

    if (cursor == null || cursor.isBlank()) {
      return null;
    }

    boolean isDesc = request.getDirection().equalsIgnoreCase("DESC");

    // 구독자 수 기준 정렬일 때
    if (request.getOrderBy().equals("subscriberCount")) {
      // 타이 브레이커를 위해 '_'로 합쳤던 cursor와 lastInterestId를 분리
      String[] parts = cursor.split("_");

      if (parts.length != 2) {
        throw new InterestException(InterestErrorCode.INVALID_CURSOR_FORMAT,
            Map.of("cursor", cursor));
      }

      long cursorCount;
      UUID cursorId;
      try {
        cursorCount = Long.parseLong(parts[0]);
        cursorId = UUID.fromString(parts[1]);
      } catch (IllegalArgumentException e) {
        throw new InterestException(InterestErrorCode.INVALID_CURSOR_FORMAT,
            Map.of("cursor", cursor));
      }

      if (isDesc) {
        return interest.subscriberCount.lt(cursorCount)
            .or(interest.subscriberCount.eq(cursorCount).and(interest.createdAt.lt(after)))
            .or(interest.subscriberCount.eq(cursorCount).and(interest.createdAt.eq(after))
                .and(interest.id.gt(cursorId)));
      }

      return interest.subscriberCount.gt(cursorCount)
          .or(interest.subscriberCount.eq(cursorCount).and(interest.createdAt.lt(after)))
          .or(interest.subscriberCount.eq(cursorCount).and(interest.createdAt.eq(after))
              .and(interest.id.gt(cursorId)));
    }

    // 이름 기준 정렬일 때
    if (isDesc) {
      return interest.name.lt(cursor);
    } else {
      return interest.name.gt(cursor);
    }
  }
}
