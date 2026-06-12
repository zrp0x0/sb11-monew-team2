package com.codeit.monew.domain.notification.repository;

import static com.codeit.monew.domain.notification.entity.QNotification.notification;

import com.codeit.monew.domain.notification.dto.NotificationSearchCondition;
import com.codeit.monew.domain.notification.entity.Notification;
import com.codeit.monew.global.dto.CursorPageResponse;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class NotificationRepositoryImpl implements NotificationRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public CursorPageResponse<Notification> searchNotifications(
        NotificationSearchCondition condition, UUID requestUserId) {

        List<Notification> notifications = queryFactory
            .selectFrom(notification)
            .where(
                notification.user.id.eq(requestUserId),
                notification.confirmed.eq(false),
                cursorCondition(condition.cursor(), condition.after())
            )
            .orderBy(notification.createdAt.asc())
            .limit(condition.limit() + 1)
            .fetch();

        boolean hasNext = notifications.size() > condition.limit();
        String nextCursor = null;
        String nextAfter = null;

        if (hasNext) {
            notifications.remove(notifications.size() - 1);

            Notification lastNotification = notifications.get(notifications.size() - 1);
            nextCursor = lastNotification.getCreatedAt().toString();
            nextAfter = lastNotification.getCreatedAt().toString();
        }

        Long totalElementCount = null;
        if (condition.cursor() == null) {
            // 첫 페이지 요청 시에만 count 쿼리 계산 - 성능 개선
            totalElementCount = Optional.ofNullable(
                queryFactory
                    .select(notification.count())
                    .from(notification)
                    .where(
                        notification.user.id.eq(requestUserId),
                        notification.confirmed.eq(false)
                    )
                    .fetchOne()
            ).orElse(0L);
        }

        return new CursorPageResponse<>(
            notifications,
            nextCursor,
            nextAfter,
            condition.limit(),
            totalElementCount,
            hasNext
        );
    }

    private BooleanExpression cursorCondition(LocalDateTime cursor, LocalDateTime after) {
        if (cursor == null || after == null) {
            return null;
        }

        // TODO: 커서 페이징 조건이 뭔가 이상함: 굳이 cursor랑 after를 받을 필요가 없는데 - 둘 다 createdAt
        // TODO: 동점차 처리 고도화 (완전히 동일한 시간인 경우)
        return notification.createdAt.gt(cursor)
            .or(notification.createdAt.eq(cursor).and(notification.createdAt.gt(after)));
    }
}
