package com.codeit.monew.domain.userActivity.repository;

import static com.codeit.monew.domain.articleView.entity.QArticleView.articleView;
import static com.codeit.monew.domain.comment.entity.QComment.comment;
import static com.codeit.monew.domain.commentLike.entity.QCommentLike.commentLike;
import static com.codeit.monew.domain.subscription.entity.QSubscription.subscription;

import com.codeit.monew.domain.userActivity.dto.UserActivityArticleViewDto;
import com.codeit.monew.domain.userActivity.dto.UserActivityCommentDto;
import com.codeit.monew.domain.userActivity.dto.UserActivityCommentLikeDto;
import com.codeit.monew.domain.userActivity.dto.UserActivitySubscriptionDto;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class UserActivityQueryRepository {

    private final JPAQueryFactory queryFactory;

    /**
     * 사용자가 구독 중인 관심사 목록을 조회
     * 관심사 이름, 키워드, 구독자 수가 필요하기 때문에 Interest를 함께 조회
     */
    public List<UserActivitySubscriptionDto> findSubscriptions(UUID userId) {
        return queryFactory
                .selectFrom(subscription)
                .join(subscription.interest).fetchJoin()
                .where(subscription.user.id.eq(userId))
                .orderBy(subscription.createdAt.desc(), subscription.id.desc())
                .fetch()
                .stream()
                .map(UserActivitySubscriptionDto::from)
                .toList();
    }

    /**
     * 사용자가 최근 작성한 댓글을 조회
     * 삭제된 댓글과 삭제된 기사에 달린 댓글은 활동 내역에서 제외
     */
    public List<UserActivityCommentDto> findRecentComments(UUID userId, int limit) {
        return queryFactory
                .selectFrom(comment)
                .join(comment.article).fetchJoin()
                .join(comment.user).fetchJoin()
                .where(
                        comment.user.id.eq(userId),
                        comment.deletedAt.isNull(),
                        comment.article.deletedAt.isNull()
                )
                .orderBy(comment.createdAt.desc(), comment.id.desc())
                .limit(limit)
                .fetch()
                .stream()
                .map(UserActivityCommentDto::from)
                .toList();
    }

    /**
     * 사용자가 최근 좋아요한 댓글을 조회
     * 좋아요 대상 댓글 또는 해당 댓글의 기사가 삭제된 경우 활동 내역에서 제외
     */
    public List<UserActivityCommentLikeDto> findRecentCommentLikes(UUID userId, int limit) {
        return queryFactory
                .selectFrom(commentLike)
                .join(commentLike.comment, comment).fetchJoin()
                .join(comment.article).fetchJoin()
                .join(comment.user).fetchJoin()
                .where(
                        commentLike.user.id.eq(userId),
                        comment.deletedAt.isNull(),
                        comment.article.deletedAt.isNull()
                )
                .orderBy(commentLike.createdAt.desc(), commentLike.id.desc())
                .limit(limit)
                .fetch()
                .stream()
                .map(UserActivityCommentLikeDto::from)
                .toList();
    }

    /**
     * 사용자가 최근 조회한 기사를 조회
     * 삭제된 기사는 최근 본 기사 목록에서 제외
     */
    public List<UserActivityArticleViewDto> findRecentArticleViews(UUID userId, int limit) {
        return queryFactory
                .selectFrom(articleView)
                .join(articleView.article).fetchJoin()
                .where(
                        articleView.user.id.eq(userId),
                        articleView.article.deletedAt.isNull()
                )
                .orderBy(articleView.createdAt.desc(), articleView.id.desc())
                .limit(limit)
                .fetch()
                .stream()
                .map(UserActivityArticleViewDto::from)
                .toList();
    }
}
