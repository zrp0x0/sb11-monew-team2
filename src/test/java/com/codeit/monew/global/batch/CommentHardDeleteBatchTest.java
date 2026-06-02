package com.codeit.monew.global.batch;

import static org.assertj.core.api.Assertions.assertThat;

import com.codeit.monew.domain.article.entity.Article;
import com.codeit.monew.domain.article.entity.ArticleSource;
import com.codeit.monew.domain.article.repository.ArticleRepository;
import com.codeit.monew.domain.comment.entity.Comment;
import com.codeit.monew.domain.comment.repository.CommentRepository;
import com.codeit.monew.domain.commentLike.entity.CommentLike;
import com.codeit.monew.domain.commentLike.repository.CommentLikeRepository;
import com.codeit.monew.domain.user.entity.User;
import com.codeit.monew.domain.user.repository.UserRepository;
import com.codeit.monew.global.config.QueryDslTestConfig;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

@DataJpaTest
@Import({QueryDslTestConfig.class, CommentHardDeleteBatchJob.class})
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = Replace.NONE)
public class CommentHardDeleteBatchTest {

  @Autowired
  private CommentHardDeleteBatchJob batchJob;

  @Autowired
  private CommentRepository commentRepository;

  @Autowired
  private CommentLikeRepository commentLikeRepository;

  @Autowired
  private ArticleRepository articleRepository;

  @Autowired
  private UserRepository userRepository;

  private Article article;
  private User user;

  @BeforeEach
  void setUp() {
    commentLikeRepository.deleteAllInBatch();
    commentRepository.deleteAllInBatch();
    articleRepository.deleteAllInBatch();
    userRepository.deleteAllInBatch();

    article = articleRepository.save(Article.create(
        ArticleSource.NAVER,
        "https://news.naver.com/test-" + UUID.randomUUID(),
        "테스트 기사 제목",
        "테스트 기사 요약",
        LocalDateTime.now()
    ));
    user = userRepository.save(User.create(
        "test-" + UUID.randomUUID() + "@example.com",
        "테스터",
        "hashPassword"
    ));
  }

  private Comment saveSoftDeletedComment(LocalDateTime deletedAt) {
    Comment comment = commentRepository.save(Comment.create(article, user, "소프트 삭제 댓글"));

    comment.softDelete();
    ReflectionTestUtils.setField(comment, "deletedAt", deletedAt);
    return commentRepository.saveAndFlush(comment);
  }

  private Comment saveActiveComment() {
    return commentRepository.saveAndFlush(Comment.create(article, user, "활성 댓글"));
  }

  private CommentLike saveCommentLike(Comment comment) {
    return commentLikeRepository.saveAndFlush(new CommentLike(comment, user));
  }

  @Test
  @DisplayName("삭제된 지 7일이 지난 댓글은 물리 삭제됨")
  void execute_shouldHardDelete_commentsOlderThan7Days() {
    Comment oldDeleted = saveSoftDeletedComment(LocalDateTime.now().minusDays(8));
    UUID oldId = oldDeleted.getId();

    batchJob.execute();

    List<UUID> remaining = commentRepository.findIdsByDeletedAtBefore(
        LocalDateTime.now().minusDays(7), 1000
    );
    assertThat(remaining).doesNotContain(oldId);
  }

  @Test
  @DisplayName("삭제된 지 7일이 지나지 않은 댓글은 물리 삭제되지 않음")
  void execute_shouldNotHardDelete_commentWithin7Days() {
    // 3일 전 소프트 삭제 댓글
    Comment recentDeleted = saveSoftDeletedComment(LocalDateTime.now().minusDays(3));
    UUID recentId = recentDeleted.getId();

    batchJob.execute();

    // 2일 기준으로 조회 시 존재
    List<UUID> ids = commentRepository.findIdsByDeletedAtBefore(
        LocalDateTime.now().minusDays(2), 1000
    );
    assertThat(ids).contains(recentId);
  }

  @Test
  @DisplayName("7일 지난 댓글의 CommentLike도 물리 삭제 시 함께 삭제됨")
  void execute_shouldAlsoDelete_commentLike() {
    Comment oldDeleted = saveSoftDeletedComment(LocalDateTime.now().minusDays(8));
    CommentLike like = saveCommentLike(oldDeleted);
    UUID likeId = like.getId();

    batchJob.execute();

    assertThat(commentLikeRepository.findById(likeId)).isEmpty();
  }

  @Test
  @DisplayName("소프트 삭제되지 않은 활성 댓글은 물리 삭제되지 않음")
  void execute_shouldNotDelete_activeComments() {
    Comment active = saveActiveComment();
    UUID activeId = active.getId();

    batchJob.execute();

    // @SQLRestriction(is_deleted = false) 조건을 충족하므로 일반 조회 가능
    assertThat(commentRepository.findById(activeId)).isPresent();
  }

  @Test
  @DisplayName("만료된 댓글과 그렇지 않은 댓글이 섞여 있을 경우, 만료된 댓글만 삭제함")
  void execute_shouldOnlyDelete_expiredComments_whenMixed() {
    Comment oldDeleted = saveSoftDeletedComment(LocalDateTime.now().minusDays(10));
    Comment recentDeleted = saveSoftDeletedComment(LocalDateTime.now().minusDays(3));
    Comment active = saveActiveComment();

    UUID oldId = oldDeleted.getId();
    UUID recentId = recentDeleted.getId();
    UUID activeId = active.getId();

    batchJob.execute();

    List<UUID> expiredRemaining = commentRepository.findIdsByDeletedAtBefore(
        LocalDateTime.now().minusDays(7), 1000
    );
    List<UUID> recentRemaining = commentRepository.findIdsByDeletedAtBefore(
        LocalDateTime.now().minusDays(2), 1000
    );

    assertThat(expiredRemaining).doesNotContain(oldId);
    assertThat(recentRemaining).contains(recentId);
    assertThat(commentRepository.findById(activeId)).isPresent();
  }

  @Test
  @DisplayName("배치 사이즈(1000건) 초과 시 반복 실행되어 모두 삭제됨")
  void execute_shouldDeleteAllInBatches_whenOver1000Comments() {
    int count = 11;
    for (int i = 0; i < count; i++) {
      saveSoftDeletedComment(LocalDateTime.now().minusDays(8));
    }

    batchJob.execute();

    List<UUID> remaining = commentRepository.findIdsByDeletedAtBefore(
        LocalDateTime.now().minusDays(7), 1000
    );

    assertThat(remaining).isEmpty();
  }
}
