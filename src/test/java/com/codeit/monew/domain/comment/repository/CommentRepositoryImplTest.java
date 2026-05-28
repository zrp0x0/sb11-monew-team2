package com.codeit.monew.domain.comment.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.codeit.monew.domain.article.entity.Article;
import com.codeit.monew.domain.article.entity.ArticleSource;
import com.codeit.monew.domain.comment.dto.CommentOrderBy;
import com.codeit.monew.domain.comment.entity.Comment;
import com.codeit.monew.domain.user.entity.User;
import com.codeit.monew.global.config.QueryDslTestConfig;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@Import(QueryDslTestConfig.class)
@ActiveProfiles("test")  // 추가
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class CommentRepositoryImplTest {

  @Autowired
  private TestEntityManager tem;

  @Autowired
  private CommentRepository commentRepository;

  private User user;
  private Article article;

  @BeforeEach
  void setUp() {
    user = tem.persistAndFlush(User.create("test@test.com", "tester", "hash"));
    article = tem.persistAndFlush(
        Article.create(ArticleSource.NAVER, "http://test.com", "title", "summary", LocalDateTime.now())
    );
  }

  @Nested
  @DisplayName("createdAt 정렬")
  class CreatedAtSort {

    @Test
    @DisplayName("커서 없이 첫 페이지 조회 시 최신순으로 limit 개수만큼 반환")
    void findComments_createdAt_firstPage() throws InterruptedException {
      tem.persistAndFlush(Comment.create(article, user, "First"));
      Thread.sleep(10);
      tem.persistAndFlush(Comment.create(article, user, "Second"));
      Thread.sleep(10);
      Comment c3 = tem.persistAndFlush(Comment.create(article, user, "Third"));

      List<Comment> result = commentRepository.findComments(
          article.getId(), CommentOrderBy.createdAt, null, null, null, 2
      );

      assertThat(result).hasSize(2);
      assertThat(result.get(0).getContent()).isEqualTo("Third");
      assertThat(result.get(1).getContent()).isEqualTo("Second");
    }

    @Test
    @DisplayName("커서 위치 다음 페이지 조회 시 커서보다 오래된 댓글만 반환")
    void findComments_createdAt_withCursor() throws InterruptedException {
      Comment c1 = tem.persistAndFlush(Comment.create(article, user, "first"));
      Thread.sleep(10);
      Comment c2 = tem.persistAndFlush(Comment.create(article, user, "second"));
      Thread.sleep(10);
      tem.persistAndFlush(Comment.create(article, user, "third"));

      List<Comment> result = commentRepository.findComments(
          article.getId(), CommentOrderBy.createdAt, c2.getCreatedAt(), c2.getId(), null, 10
      );

      assertThat(result).hasSize(1);
      assertThat(result.get(0).getContent()).isEqualTo("first");
    }

    @Test
    @DisplayName("limit보다 댓글이 많으면 limit 개수만큼 반환")
    void findComments_createdAt_limitApplied() throws InterruptedException {
      for (int i = 1; i <= 5; i++) {
        tem.persistAndFlush(Comment.create(article, user, "comment" + i));
        Thread.sleep(10);
      }
      List<Comment> result = commentRepository.findComments(
          article.getId(), CommentOrderBy.createdAt, null, null, null, 3
      );

      assertThat(result).hasSize(3);
    }
  }

  @Nested
  @DisplayName("likeCount 정렬")
  class LikeCountSort {

    @Test
    @DisplayName("커서 없이 첫 페이지 조회 시 좋아요 순으로 반환")
    void findComments_likeCount_firstPage() {
      Comment c1 = Comment.create(article, user, "first");
      c1.increaseLikeCount();
      tem.persistAndFlush(c1);

      Comment c2 = Comment.create(article, user, "second");
      c2.increaseLikeCount();
      c2.increaseLikeCount();
      c2.increaseLikeCount();
      tem.persistAndFlush(c2);

      Comment c3 = Comment.create(article, user, "third");
      c3.increaseLikeCount();
      c3.increaseLikeCount();
      tem.persistAndFlush(c3);

      List<Comment> result = commentRepository.findComments(
          article.getId(), CommentOrderBy.likeCount, null, null, null, 2
      );

      assertThat(result).hasSize(2);
      assertThat(result.get(0).getContent()).isEqualTo("second");
      assertThat(result.get(1).getContent()).isEqualTo("third");
    }

    @Test
    @DisplayName("커서 이후 페이지 조회 시 커서 likeCount 미만 댓글 반환")
    void findComments_likeCount_withCursor() throws InterruptedException {
      Comment c1 = Comment.create(article, user, "first");
      c1.increaseLikeCount();
      tem.persistAndFlush(c1);
      Thread.sleep(10);

      Comment c2 = Comment.create(article, user, "second");
      c2.increaseLikeCount();
      c2.increaseLikeCount();
      c2.increaseLikeCount();
      tem.persistAndFlush(c2);
      Thread.sleep(10);

      Comment c3 = Comment.create(article, user, "third");
      c3.increaseLikeCount();
      c3.increaseLikeCount();
      tem.persistAndFlush(c3);

      List<Comment> result = commentRepository.findComments(
          article.getId(), CommentOrderBy.likeCount, c3.getCreatedAt(), null, c3.getLikeCounts(), 10
      );

      assertThat(result).hasSize(1);
      assertThat(result.get(0).getContent()).isEqualTo("first");
    }

    @Test
    @DisplayName("좋아요 수가 같을 경우 createdAt 최신순으로 반환")
    void findComments_likeCount_tieBreaker() throws InterruptedException {
      Comment c1 = Comment.create(article, user, "first");
      c1.increaseLikeCount();
      tem.persistAndFlush(c1);
      Thread.sleep(10);

      Comment c2 = Comment.create(article, user, "second");
      c2.increaseLikeCount();
      tem.persistAndFlush(c2);

      List<Comment> result = commentRepository.findComments(
          article.getId(), CommentOrderBy.likeCount, null, null, null, 10
      );

      assertThat(result).hasSize(2);
      assertThat(result.get(0).getContent()).isEqualTo("second");
      assertThat(result.get(1).getContent()).isEqualTo("first");
    }
  }
}
