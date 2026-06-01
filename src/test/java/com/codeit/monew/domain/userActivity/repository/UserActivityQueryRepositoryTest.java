package com.codeit.monew.domain.userActivity.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.codeit.monew.domain.article.entity.Article;
import com.codeit.monew.domain.article.entity.ArticleSource;
import com.codeit.monew.domain.articleView.entity.ArticleView;
import com.codeit.monew.domain.comment.entity.Comment;
import com.codeit.monew.domain.commentLike.entity.CommentLike;
import com.codeit.monew.domain.interest.entity.Interest;
import com.codeit.monew.domain.subscription.entity.Subscription;
import com.codeit.monew.domain.user.entity.User;
import com.codeit.monew.domain.userActivity.dto.UserActivityArticleViewDto;
import com.codeit.monew.domain.userActivity.dto.UserActivityCommentDto;
import com.codeit.monew.domain.userActivity.dto.UserActivityCommentLikeDto;
import com.codeit.monew.domain.userActivity.dto.UserActivitySubscriptionDto;
import com.codeit.monew.global.config.JpaAuditingConfig;
import com.codeit.monew.global.config.QuerydslConfig;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@Import({
        JpaAuditingConfig.class,
        QuerydslConfig.class,
        UserActivityQueryRepository.class
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UserActivityQueryRepositoryTest {

    @Autowired
    TestEntityManager entityManager;

    @Autowired
    UserActivityQueryRepository userActivityQueryRepository;

    @Test
    @DisplayName("사용자가 구독 중인 관심사 목록을 최신순으로 조회")
    void findSubscriptions_success() {
        // given
        User user = persistUser("subscriber@example.com");
        User otherUser = persistUser("other-subscriber@example.com");
        Interest firstInterest = persistInterest("interest-a", List.of("keyword-a"));
        Interest secondInterest = persistInterest("interest-b", List.of("keyword-b"));
        Interest otherInterest = persistInterest("interest-c", List.of("keyword-c"));
        Subscription firstSubscription = persistSubscription(
                firstInterest,
                user,
                LocalDateTime.of(2026, 6, 1, 10, 0)
        );
        Subscription secondSubscription = persistSubscription(
                secondInterest,
                user,
                LocalDateTime.of(2026, 6, 1, 11, 0)
        );
        persistSubscription(otherInterest, otherUser, LocalDateTime.of(2026, 6, 1, 12, 0));
        flushAndClear();

        // when
        List<UserActivitySubscriptionDto> result = userActivityQueryRepository.findSubscriptions(user.getId());

        // then
        assertThat(result).hasSize(2)
                .extracting(UserActivitySubscriptionDto::id)
                .containsExactly(secondSubscription.getId(), firstSubscription.getId());
        assertThat(result.get(0).interestId()).isEqualTo(secondInterest.getId());
        assertThat(result.get(0).interestKeywords()).containsExactly("keyword-b");
    }

    @Test
    @DisplayName("사용자가 최근 작성한 댓글은 삭제된 댓글과 삭제된 기사 댓글을 제외하고 조회")
    void findRecentComments_excludesDeletedCommentAndDeletedArticle() {
        // given
        User user = persistUser("commenter@example.com");
        User otherUser = persistUser("other-commenter@example.com");
        Article article = persistArticle("https://news.example.com/comment-article");
        Article deletedArticle = persistArticle("https://news.example.com/deleted-comment-article");
        deletedArticle.softDelete();

        Comment oldComment = persistComment(
                article,
                user,
                "old comment",
                LocalDateTime.of(2026, 6, 1, 10, 0)
        );
        Comment latestComment = persistComment(
                article,
                user,
                "latest comment",
                LocalDateTime.of(2026, 6, 1, 11, 0)
        );
        Comment deletedComment = persistComment(
                article,
                user,
                "deleted comment",
                LocalDateTime.of(2026, 6, 1, 12, 0)
        );
        deletedComment.softDelete();
        persistComment(deletedArticle, user, "deleted article comment", LocalDateTime.of(2026, 6, 1, 13, 0));
        persistComment(article, otherUser, "other user comment", LocalDateTime.of(2026, 6, 1, 14, 0));
        flushAndClear();

        // when
        List<UserActivityCommentDto> result = userActivityQueryRepository.findRecentComments(user.getId(), 10);

        // then
        assertThat(result).hasSize(2)
                .extracting(UserActivityCommentDto::id)
                .containsExactly(latestComment.getId(), oldComment.getId());
        assertThat(result)
                .extracting(UserActivityCommentDto::content)
                .containsExactly("latest comment", "old comment");
    }

    @Test
    @DisplayName("사용자가 최근 좋아요한 댓글은 삭제된 댓글과 삭제된 기사 댓글을 제외하고 limit만큼 조회")
    void findRecentCommentLikes_excludesDeletedTargetsAndAppliesLimit() {
        // given
        User user = persistUser("comment-like-user@example.com");
        User writer = persistUser("comment-like-writer@example.com");
        Article article = persistArticle("https://news.example.com/comment-like-article");
        Article deletedArticle = persistArticle("https://news.example.com/deleted-comment-like-article");
        deletedArticle.softDelete();

        Comment firstComment = persistComment(article, writer, "first liked comment", LocalDateTime.of(2026, 6, 1, 9, 0));
        Comment secondComment = persistComment(article, writer, "second liked comment", LocalDateTime.of(2026, 6, 1, 10, 0));
        Comment deletedComment = persistComment(article, writer, "deleted liked comment", LocalDateTime.of(2026, 6, 1, 11, 0));
        deletedComment.softDelete();
        Comment deletedArticleComment = persistComment(
                deletedArticle,
                writer,
                "deleted article liked comment",
                LocalDateTime.of(2026, 6, 1, 12, 0)
        );

        CommentLike firstLike = persistCommentLike(firstComment, user, LocalDateTime.of(2026, 6, 1, 13, 0));
        CommentLike secondLike = persistCommentLike(secondComment, user, LocalDateTime.of(2026, 6, 1, 14, 0));
        persistCommentLike(deletedComment, user, LocalDateTime.of(2026, 6, 1, 15, 0));
        persistCommentLike(deletedArticleComment, user, LocalDateTime.of(2026, 6, 1, 16, 0));
        flushAndClear();

        // when
        List<UserActivityCommentLikeDto> result = userActivityQueryRepository.findRecentCommentLikes(user.getId(), 1);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(secondLike.getId());
        assertThat(result.get(0).id()).isNotEqualTo(firstLike.getId());
        assertThat(result.get(0).commentContent()).isEqualTo("second liked comment");
    }

    @Test
    @DisplayName("사용자가 최근 조회한 기사는 삭제된 기사를 제외하고 limit만큼 조회")
    void findRecentArticleViews_excludesDeletedArticlesAndAppliesLimit() {
        // given
        User user = persistUser("article-view-user@example.com");
        Article firstArticle = persistArticle("https://news.example.com/article-view-first");
        Article secondArticle = persistArticle("https://news.example.com/article-view-second");
        Article deletedArticle = persistArticle("https://news.example.com/article-view-deleted");
        deletedArticle.softDelete();

        ArticleView firstView = persistArticleView(firstArticle, user, LocalDateTime.of(2026, 6, 1, 10, 0));
        ArticleView secondView = persistArticleView(secondArticle, user, LocalDateTime.of(2026, 6, 1, 11, 0));
        persistArticleView(deletedArticle, user, LocalDateTime.of(2026, 6, 1, 12, 0));
        flushAndClear();

        // when
        List<UserActivityArticleViewDto> result = userActivityQueryRepository.findRecentArticleViews(user.getId(), 1);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(secondView.getId());
        assertThat(result.get(0).id()).isNotEqualTo(firstView.getId());
        assertThat(result.get(0).articleId()).isEqualTo(secondArticle.getId());
    }

    private User persistUser(String email) {
        return entityManager.persistAndFlush(User.create(
                email,
                "nickname",
                "$2y$04$CnmQ.L0MoRdQxDev/JnKaOKKDqae5Ja40NMIgep0h7xRbX6jhRzZm"
        ));
    }

    private Interest persistInterest(String name, List<String> keywords) {
        return entityManager.persistAndFlush(Interest.create(name, keywords));
    }

    private Article persistArticle(String sourceUrl) {
        return entityManager.persistAndFlush(Article.create(
                ArticleSource.NAVER,
                sourceUrl,
                "article title",
                "article summary",
                LocalDateTime.of(2026, 6, 1, 8, 0)
        ));
    }

    private Subscription persistSubscription(Interest interest, User user, LocalDateTime createdAt) {
        Subscription subscription = entityManager.persistAndFlush(Subscription.create(interest, user));
        updateCreatedAt("Subscription", subscription.getId(), createdAt);
        return subscription;
    }

    private Comment persistComment(Article article, User user, String content, LocalDateTime createdAt) {
        Comment comment = entityManager.persistAndFlush(Comment.create(article, user, content));
        updateCreatedAt("Comment", comment.getId(), createdAt);
        return comment;
    }

    private CommentLike persistCommentLike(Comment comment, User user, LocalDateTime createdAt) {
        CommentLike commentLike = entityManager.persistAndFlush(new CommentLike(comment, user));
        updateCreatedAt("CommentLike", commentLike.getId(), createdAt);
        return commentLike;
    }

    private ArticleView persistArticleView(Article article, User user, LocalDateTime createdAt) {
        ArticleView articleView = entityManager.persistAndFlush(ArticleView.create(user, article));
        updateCreatedAt("ArticleView", articleView.getId(), createdAt);
        return articleView;
    }

    private void updateCreatedAt(String entityName, UUID id, LocalDateTime createdAt) {
        entityManager.getEntityManager()
                .createQuery("UPDATE " + entityName + " e SET e.createdAt = :createdAt WHERE e.id = :id")
                .setParameter("createdAt", createdAt)
                .setParameter("id", id)
                .executeUpdate();
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }
}
