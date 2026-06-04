package com.codeit.monew.domain.userActivity.service;

import com.codeit.monew.domain.userActivity.dto.UserActivityDto;
import com.codeit.monew.domain.userActivity.repository.UserActivityQueryRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class PostgresUserActivityReader implements UserActivityReader {

    private static final int RECENT_ACTIVITY_LIMIT = 10;

    private final UserActivityQueryRepository userActivityQueryRepository;

    @Override
    @Transactional(readOnly = true)
    public UserActivityDto read(UUID userId) {
        return new UserActivityDto(
                userId,
                userActivityQueryRepository.findSubscriptions(userId),
                userActivityQueryRepository.findRecentComments(userId, RECENT_ACTIVITY_LIMIT),
                userActivityQueryRepository.findRecentCommentLikes(userId, RECENT_ACTIVITY_LIMIT),
                userActivityQueryRepository.findRecentArticleViews(userId, RECENT_ACTIVITY_LIMIT)
        );
    }
}
