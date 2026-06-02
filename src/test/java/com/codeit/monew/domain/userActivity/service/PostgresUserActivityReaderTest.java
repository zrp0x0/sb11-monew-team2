package com.codeit.monew.domain.userActivity.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.codeit.monew.domain.userActivity.dto.UserActivityArticleViewDto;
import com.codeit.monew.domain.userActivity.dto.UserActivityCommentDto;
import com.codeit.monew.domain.userActivity.dto.UserActivityCommentLikeDto;
import com.codeit.monew.domain.userActivity.dto.UserActivityDto;
import com.codeit.monew.domain.userActivity.dto.UserActivitySubscriptionDto;
import com.codeit.monew.domain.userActivity.repository.UserActivityQueryRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PostgresUserActivityReaderTest {

    @Mock
    UserActivityQueryRepository userActivityQueryRepository;

    @InjectMocks
    PostgresUserActivityReader postgresUserActivityReader;

    @Test
    @DisplayName("PostgreSQL 조회 결과를 사용자 활동 내역 응답으로 조립")
    void read_success() {
        // given
        UUID userId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        List<UserActivitySubscriptionDto> subscriptions = List.of();
        List<UserActivityCommentDto> comments = List.of();
        List<UserActivityCommentLikeDto> commentLikes = List.of();
        List<UserActivityArticleViewDto> articleViews = List.of();

        when(userActivityQueryRepository.findSubscriptions(userId)).thenReturn(subscriptions);
        when(userActivityQueryRepository.findRecentComments(userId, 10)).thenReturn(comments);
        when(userActivityQueryRepository.findRecentCommentLikes(userId, 10)).thenReturn(commentLikes);
        when(userActivityQueryRepository.findRecentArticleViews(userId, 10)).thenReturn(articleViews);

        // when
        UserActivityDto response = postgresUserActivityReader.read(userId);

        // then
        assertThat(response.userId()).isEqualTo(userId);
        assertThat(response.subscriptions()).isSameAs(subscriptions);
        assertThat(response.comments()).isSameAs(comments);
        assertThat(response.commentLikes()).isSameAs(commentLikes);
        assertThat(response.articleViews()).isSameAs(articleViews);

        verify(userActivityQueryRepository).findSubscriptions(userId);
        verify(userActivityQueryRepository).findRecentComments(userId, 10);
        verify(userActivityQueryRepository).findRecentCommentLikes(userId, 10);
        verify(userActivityQueryRepository).findRecentArticleViews(userId, 10);
    }
}
