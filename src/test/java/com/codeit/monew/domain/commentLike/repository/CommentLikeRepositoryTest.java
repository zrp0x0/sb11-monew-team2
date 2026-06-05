package com.codeit.monew.domain.commentLike.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.codeit.monew.domain.article.entity.Article;
import com.codeit.monew.domain.article.entity.ArticleSource;
import com.codeit.monew.domain.comment.entity.Comment;
import com.codeit.monew.domain.commentLike.entity.CommentLike;
import com.codeit.monew.domain.user.entity.User;
import com.codeit.monew.global.config.QueryDslTestConfig;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
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
@Import(QueryDslTestConfig.class)
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class CommentLikeRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private CommentLikeRepository commentLikeRepository;

    @Test
    @DisplayName("commentId와 userId로 CommentLike를 조회한다")
    void findByCommentIdAndUserId_returnsCommentLike() {
        User commentUser = persistUser("commenter@example.com", "commenter");
        User likedUser = persistUser("liked@example.com", "likedUser");
        Article article = persistArticle("https://news.example.com/comment-like-find");
        Comment comment = persistComment(article, commentUser, "댓글입니다");
        CommentLike commentLike = persistCommentLike(comment, likedUser);
        entityManager.clear();

        Optional<CommentLike> result = commentLikeRepository.findByCommentIdAndUserId(
                comment.getId(),
                likedUser.getId()
        );

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(commentLike.getId());
        assertThat(result.get().getComment().getId()).isEqualTo(comment.getId());
        assertThat(result.get().getUser().getId()).isEqualTo(likedUser.getId());
        assertThat(result.get().getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("commentId와 userId에 해당하는 CommentLike가 없으면 빈 Optional을 반환한다")
    void findByCommentIdAndUserId_returnsEmpty() {
        User commentUser = persistUser("commenter-empty@example.com", "commenter");
        User likedUser = persistUser("liked-empty@example.com", "likedUser");
        User otherUser = persistUser("other-empty@example.com", "otherUser");
        Article article = persistArticle("https://news.example.com/comment-like-empty");
        Comment comment = persistComment(article, commentUser, "댓글입니다");
        persistCommentLike(comment, likedUser);
        entityManager.clear();

        Optional<CommentLike> result = commentLikeRepository.findByCommentIdAndUserId(
                comment.getId(),
                otherUser.getId()
        );

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("사용자가 좋아요한 댓글 ID만 목록에서 필터링해 조회한다")
    void findByUserIdAndCommentIdIn_returnsOnlyLikedCommentIds() {
        User commentUser = persistUser("commenter-list@example.com", "commenter");
        User likedUser = persistUser("liked-list@example.com", "likedUser");
        User otherUser = persistUser("other-list@example.com", "otherUser");
        Article article = persistArticle("https://news.example.com/comment-like-list");
        Comment likedComment1 = persistComment(article, commentUser, "좋아요한 댓글 1");
        Comment likedComment2 = persistComment(article, commentUser, "좋아요한 댓글 2");
        Comment notLikedComment = persistComment(article, commentUser, "좋아요하지 않은 댓글");
        Comment otherUserLikedComment = persistComment(article, commentUser, "다른 사용자 좋아요 댓글");
        persistCommentLike(likedComment1, likedUser);
        persistCommentLike(likedComment2, likedUser);
        persistCommentLike(otherUserLikedComment, otherUser);
        entityManager.clear();

        List<UUID> result = commentLikeRepository.findByUserIdAndCommentIdIn(
                likedUser.getId(),
                List.of(
                        likedComment1.getId(),
                        likedComment2.getId(),
                        notLikedComment.getId(),
                        otherUserLikedComment.getId()
                )
        );

        assertThat(result).containsExactlyInAnyOrder(likedComment1.getId(), likedComment2.getId());
    }

    @Test
    @DisplayName("commentId로 CommentLike를 모두 삭제한다")
    void deleteAllByCommentId_deletesLikesForComment() {
        User commentUser = persistUser("commenter-delete@example.com", "commenter");
        User likedUser1 = persistUser("liked-delete-1@example.com", "likedUser1");
        User likedUser2 = persistUser("liked-delete-2@example.com", "likedUser2");
        Article article = persistArticle("https://news.example.com/comment-like-delete");
        Comment targetComment = persistComment(article, commentUser, "삭제 대상 댓글");
        Comment remainComment = persistComment(article, commentUser, "남아야 하는 댓글");
        persistCommentLike(targetComment, likedUser1);
        persistCommentLike(targetComment, likedUser2);
        CommentLike remainLike = persistCommentLike(remainComment, likedUser1);
        entityManager.clear();

        commentLikeRepository.deleteAllByCommentId(targetComment.getId());
        commentLikeRepository.flush();
        entityManager.clear();

        assertThat(commentLikeRepository.findAll())
                .extracting(CommentLike::getId)
                .containsExactly(remainLike.getId());
    }

    @Test
    @DisplayName("여러 commentId에 해당하는 CommentLike를 native query로 물리 삭제한다")
    void hardDeleteAllByCommentIdIn_deletesLikesForComments() {
        User commentUser = persistUser("commenter-hard-delete@example.com", "commenter");
        User likedUser = persistUser("liked-hard-delete@example.com", "likedUser");
        Article article = persistArticle("https://news.example.com/comment-like-hard-delete");
        Comment targetComment1 = persistComment(article, commentUser, "삭제 대상 댓글 1");
        Comment targetComment2 = persistComment(article, commentUser, "삭제 대상 댓글 2");
        Comment remainComment = persistComment(article, commentUser, "남아야 하는 댓글");
        persistCommentLike(targetComment1, likedUser);
        persistCommentLike(targetComment2, likedUser);
        CommentLike remainLike = persistCommentLike(remainComment, likedUser);
        entityManager.clear();

        commentLikeRepository.hardDeleteAllByCommentIdIn(
                List.of(targetComment1.getId(), targetComment2.getId())
        );
        commentLikeRepository.flush();
        entityManager.clear();

        assertThat(commentLikeRepository.findAll())
                .extracting(CommentLike::getId)
                .containsExactly(remainLike.getId());
    }

    private User persistUser(String email, String nickname) {
        return entityManager.persistAndFlush(User.create(email, nickname, "password-hash"));
    }

    private Article persistArticle(String sourceUrl) {
        return entityManager.persistAndFlush(
                Article.create(
                        ArticleSource.NAVER,
                        sourceUrl,
                        "title",
                        "summary",
                        LocalDateTime.of(2026, 6, 1, 10, 0)
                )
        );
    }

    private Comment persistComment(Article article, User user, String content) {
        return entityManager.persistAndFlush(Comment.create(article, user, content));
    }

    private CommentLike persistCommentLike(Comment comment, User user) {
        return entityManager.persistAndFlush(new CommentLike(comment, user));
    }
}
