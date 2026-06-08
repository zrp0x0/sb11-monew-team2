package com.codeit.monew.domain.comment.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.codeit.monew.domain.article.entity.Article;
import com.codeit.monew.domain.article.entity.ArticleSource;
import com.codeit.monew.domain.comment.dto.CommentDto;
import com.codeit.monew.domain.comment.dto.CommentSearchRequest;
import com.codeit.monew.domain.comment.entity.Comment;
import com.codeit.monew.domain.user.entity.User;
import com.codeit.monew.global.config.QueryDslTestConfig;
import com.codeit.monew.global.dto.CursorPageResponse;
import java.time.LocalDateTime;
import java.util.UUID;
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
  private UUID requestUserId;

  @BeforeEach
  void setUp() {
    user = tem.persistAndFlush(User.create("test@test.com", "tester", "hash"));
    article = tem.persistAndFlush(
        Article.create(ArticleSource.NAVER, "http://test.com", "title", "summary", LocalDateTime.now())
    );
    requestUserId = user.getId();
  }

  private String buildCreatedAtCursor(Comment comment) {
    return comment.getId().toString();
  }

  private String buildLikeCountCursor(Comment comment) {
    return comment.getLikeCounts() + "_" + comment.getId();
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
      tem.persistAndFlush(Comment.create(article, user, "Third"));

      CommentSearchRequest request = new CommentSearchRequest(
          article.getId(), null, null, 2, "createdAt", "DESC"
      );

      CursorPageResponse<CommentDto> result = commentRepository.findComments(request, requestUserId);

      assertThat(result.content()).hasSize(2);
      assertThat(result.content().get(0).content()).isEqualTo("Third");
      assertThat(result.content().get(1).content()).isEqualTo("Second");
      assertThat(result.hasNext()).isTrue();
      assertThat(result.totalElements()).isEqualTo(3L);
      assertThat(result.nextCursor()).isNotNull();
      assertThat(result.nextAfter()).isNotNull();
    }

    @Test
    @DisplayName("커서 위치 다음 페이지 조회 시 커서보다 오래된 댓글만 반환")
    void findComments_createdAt_withCursor() throws InterruptedException {
      Comment c1 = tem.persistAndFlush(Comment.create(article, user, "first"));
      Thread.sleep(10);
      Comment c2 = tem.persistAndFlush(Comment.create(article, user, "second"));
      Thread.sleep(10);
      tem.persistAndFlush(Comment.create(article, user, "third"));

      CommentSearchRequest request = new CommentSearchRequest(
          article.getId(), buildCreatedAtCursor(c2), c2.getCreatedAt(), 10, "createdAt", "DESC"
      );

      CursorPageResponse<CommentDto> result = commentRepository.findComments(request, requestUserId);

      assertThat(result.content()).hasSize(1);
      assertThat(result.content().get(0).content()).isEqualTo("first");
      assertThat(result.hasNext()).isFalse();
      assertThat(result.nextCursor()).isNull();
      assertThat(result.nextAfter()).isNull();
    }

    @Test
    @DisplayName("limit보다 댓글이 많으면 limit 개수만큼 반환")
    void findComments_createdAt_limitApplied() throws InterruptedException {
      for (int i = 1; i <= 5; i++) {
        tem.persistAndFlush(Comment.create(article, user, "comment" + i));
        Thread.sleep(10);
      }
      CommentSearchRequest request = new CommentSearchRequest(
          article.getId(), null, null, 3, "createdAt", "DESC"
      );

      CursorPageResponse<CommentDto> result = commentRepository.findComments(request, requestUserId);

      assertThat(result.content()).hasSize(3);
      assertThat(result.hasNext()).isTrue();
    }

    @Test
    @DisplayName("nextCursor와 nextAfter가 올바른 형식으로 반환됨")
    void findComments_createdAt_nextCursorFormat() throws InterruptedException {
      Comment c1 = tem.persistAndFlush(Comment.create(article, user, "first"));
      Thread.sleep(10);
      Comment c2 = tem.persistAndFlush(Comment.create(article, user, "second"));

      CommentSearchRequest request = new CommentSearchRequest(
          article.getId(), null, null, 1, "createdAt", "DESC"
      );

      CursorPageResponse<CommentDto> result = commentRepository.findComments(request, requestUserId);

      assertThat(result.nextCursor()).isEqualTo(c2.getId().toString());
      assertThat(result.nextAfter()).isNotNull();
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

      CommentSearchRequest request = new CommentSearchRequest(
          article.getId(), null, null, 2, "likeCount", "DESC"
      );

      CursorPageResponse<CommentDto> result = commentRepository.findComments(request, requestUserId);

      assertThat(result.content()).hasSize(2);
      assertThat(result.content().get(0).content()).isEqualTo("second");
      assertThat(result.content().get(1).content()).isEqualTo("third");
      assertThat(result.hasNext()).isTrue();
      assertThat(result.nextCursor()).isEqualTo(c3.getLikeCounts() + "_" +c3.getId());
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

      CommentSearchRequest request = new CommentSearchRequest(
          article.getId(), buildLikeCountCursor(c3), c3.getCreatedAt(), 10, "likeCount", "DESC"
      );

      CursorPageResponse<CommentDto> result = commentRepository.findComments(request, requestUserId);

      assertThat(result.content()).hasSize(1);
      assertThat(result.content().get(0).content()).isEqualTo("first");
      assertThat(result.hasNext()).isFalse();
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

      CommentSearchRequest request = new CommentSearchRequest(
          article.getId(), null, null, 10, "likeCount", "DESC"
      );

      CursorPageResponse<CommentDto> result = commentRepository.findComments(request, requestUserId);

      assertThat(result.content()).hasSize(2);
      assertThat(result.content().get(0).content()).isEqualTo("second");
      assertThat(result.content().get(1).content()).isEqualTo("first");
    }
  }
}
