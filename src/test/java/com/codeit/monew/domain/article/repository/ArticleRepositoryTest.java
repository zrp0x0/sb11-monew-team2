package com.codeit.monew.domain.article.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.codeit.monew.domain.article.dto.request.ArticleSearchRequest;
import com.codeit.monew.domain.article.entity.Article;
import com.codeit.monew.domain.article.entity.ArticleSource;
import com.codeit.monew.global.config.QuerydslConfig;
import com.codeit.monew.global.dto.CursorPageResponse;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({
    QuerydslConfig.class,
    ArticleRepositoryTest.JpaAuditingTestConfig.class
})
@Transactional
class ArticleRepositoryTest {

  @Autowired
  private EntityManager em;

  @Autowired
  private ArticleRepository articleRepository;

  private static final DateTimeFormatter CURSOR_DATE_FORMATTER = DateTimeFormatter.ofPattern(
      "yyyy-MM-dd'T'HH:mm:ss.SSSSSS");

  @Test
  @DisplayName("Upsert 쿼리 - 중복 기사(source_url)는 무시되고 새로운 기사는 저장됨")
  void upsertArticleSkipDuplicate_Success() {
    // given
    String targetUrl = "http://test.com/url";

    Article existingArticle = Article.create(ArticleSource.NAVER, targetUrl, "기존 기사", "요약",
        LocalDateTime.now());
    articleRepository.save(existingArticle);
    em.flush();
    em.clear();

    Article duplicateArticle = Article.restore(
        UUID.randomUUID(), ArticleSource.NAVER, targetUrl, "중복 기사", "요약", LocalDateTime.now(), LocalDateTime.now());

    Article newArticle = Article.restore(
        UUID.randomUUID(), ArticleSource.NAVER, "http://new-url.com", "새 기사", "요약",
        LocalDateTime.now(), LocalDateTime.now());

    // when
    int duplicateResult = articleRepository.upsertArticleSkipDuplicate(duplicateArticle);
    int newResult = articleRepository.upsertArticleSkipDuplicate(newArticle);
    em.flush();
    em.clear();

    // then
    assertThat(duplicateResult).isEqualTo(0); // 중복이므로 0행 처리함
    assertThat(newResult).isEqualTo(1); // 새 기사이므로 1행 처리함
    assertThat(articleRepository.count()).isEqualTo(2);
  }

  @Test
  @DisplayName("sourceIn 조건으로 뉴스 기사 목록을 필터링함")
  void searchArticles_filterBySourceIn() {
    // given
    Article naverArticle = Article.create(
        ArticleSource.NAVER,
        "https://news.naver.com/source-filter",
        "네이버 기사 제목",
        "네이버 기사 요약",
        LocalDateTime.of(2026, 5, 27, 10, 30)
    );

    Article hankyungArticle = Article.create(
        ArticleSource.HANKYUNG,
        "https://www.hankyung.com/source-filter",
        "한국경제 기사 제목",
        "한국경제 기사 요약",
        LocalDateTime.of(2026, 5, 27, 11, 30)
    );

    articleRepository.saveAllAndFlush(List.of(naverArticle, hankyungArticle));

    ArticleSearchRequest request = new ArticleSearchRequest(
        null,
        null,
        List.of(ArticleSource.NAVER),
        null,
        null,
        "publishDate",
        "DESC",
        null,
        null,
        10,
        UUID.randomUUID()
    );

    // when
    CursorPageResponse<Article> response = articleRepository.searchArticles(request);

    // then
    assertThat(response.content()).hasSize(1);
    assertThat(response.content().get(0).getSource()).isEqualTo(ArticleSource.NAVER);
    assertThat(response.content().get(0).getTitle()).isEqualTo("네이버 기사 제목");
    assertThat(response.hasNext()).isFalse();
  }

  @Test
  @DisplayName("keyword가 제목 또는 요약에 포함된 뉴스 기사를 조회함")
  void searchArticles_filterByKeyword() {
    // given
    Article matchedByTitle = Article.create(
        ArticleSource.NAVER,
        "https://news.naver.com/keyword-title",
        "AI 반도체 시장 성장",
        "테스트 요약",
        LocalDateTime.of(2026, 5, 27, 10, 30)
    );

    Article matchedBySummary = Article.create(
        ArticleSource.CHOSUN,
        "https://www.chosun.com/keyword-summary",
        "경제 뉴스",
        "AI 관련 투자 확대",
        LocalDateTime.of(2026, 5, 27, 11, 30)
    );

    Article notMatched = Article.create(
        ArticleSource.YEONHAP,
        "https://www.yna.co.kr/keyword-no-match",
        "스포츠 뉴스",
        "축구 경기 결과",
        LocalDateTime.of(2026, 5, 27, 12, 30)
    );

    articleRepository.saveAllAndFlush(List.of(matchedByTitle, matchedBySummary, notMatched));

    ArticleSearchRequest request = new ArticleSearchRequest(
        "AI",
        null,
        null,
        null,
        null,
        "publishDate",
        "DESC",
        null,
        null,
        10,
        UUID.randomUUID()
    );

    // when
    CursorPageResponse<Article> response = articleRepository.searchArticles(request);

    // then
    assertThat(response.content()).hasSize(2);
    assertThat(response.content())
        .extracting(Article::getTitle)
        .containsExactly("경제 뉴스", "AI 반도체 시장 성장");
  }

  @Test
  @DisplayName("경제적 범위 조건으로 뉴스 기사 목록을 필터링함")
  void searchArticles_filterByPublishDateRange() {
    // given
    Article oldArticle = Article.create(
        ArticleSource.NAVER,
        "https://news.naver.com/date-old",
        "오래된 기사",
        "오래된 기사 요약",
        LocalDateTime.of(2026, 5, 1, 10, 30)
    );

    Article targetArticle = Article.create(
        ArticleSource.NAVER,
        "https://news.naver.com/date-target",
        "범위 안 기사",
        "범위 안 기사 요약",
        LocalDateTime.of(2026, 5, 20, 10, 30)
    );

    Article futureArticle = Article.create(
        ArticleSource.NAVER,
        "https://news.naver.com/date-future",
        "미래 기사",
        "미래 기사 요약",
        LocalDateTime.of(2026, 5, 30, 10, 30)
    );

    articleRepository.saveAllAndFlush(List.of(oldArticle, targetArticle, futureArticle));

    ArticleSearchRequest request = new ArticleSearchRequest(
        null,
        null,
        null,
        LocalDateTime.of(2026, 5, 10, 0, 0),
        LocalDateTime.of(2026, 5, 25, 23, 59),
        "publishDate",
        "DESC",
        null,
        null,
        10,
        UUID.randomUUID()
    );

    // when
    CursorPageResponse<Article> response = articleRepository.searchArticles(request);

    // then
    assertThat(response.content()).hasSize(1);
    assertThat(response.content().get(0).getTitle()).isEqualTo("범위 안 기사");
  }

  @Test
  @DisplayName("publishDate 기준 내림차순으로 뉴스 기사 목록을 정렬함")
  void searchArticles_orderByPublishDateDesc() {
    // given
    Article firstArticle = Article.create(
        ArticleSource.NAVER,
        "https://news.naver.com/order-desc-first",
        "먼저 발행된 기사",
        "요약",
        LocalDateTime.of(2026, 5, 27, 10, 30)
    );

    Article secondArticle = Article.create(
        ArticleSource.NAVER,
        "https://news.naver.com/order-desc-second",
        "나중에 발행된 기사",
        "요약",
        LocalDateTime.of(2026, 5, 27, 11, 30)
    );

    articleRepository.saveAllAndFlush(List.of(firstArticle, secondArticle));

    ArticleSearchRequest request = new ArticleSearchRequest(
        null,
        null,
        null,
        null,
        null,
        "publishDate",
        "DESC",
        null,
        null,
        10,
        UUID.randomUUID()
    );

    // when
    CursorPageResponse<Article> response = articleRepository.searchArticles(request);

    // then
    assertThat(response.content())
        .extracting(Article::getTitle)
        .containsExactly("나중에 발행된 기사", "먼저 발행된 기사");
  }

  @Test
  @DisplayName("publishDate 기준 오름차순으로 뉴스 기사 목록을 정렬함")
  void searchArticles_orderByPublishDateAsc() {
    // given
    Article firstArticle = Article.create(
        ArticleSource.NAVER,
        "https://news.naver.com/order-asc-first",
        "먼저 발행된 기사",
        "요약",
        LocalDateTime.of(2026, 5, 27, 10, 30)
    );

    Article secondArticle = Article.create(
        ArticleSource.NAVER,
        "https://news.naver.com/order-asc-second",
        "나중에 발행된 기사",
        "요약",
        LocalDateTime.of(2026, 5, 27, 11, 30)
    );

    articleRepository.saveAllAndFlush(List.of(firstArticle, secondArticle));

    ArticleSearchRequest request = new ArticleSearchRequest(
        null,
        null,
        null,
        null,
        null,
        "publishDate",
        "ASC",
        null,
        null,
        10,
        UUID.randomUUID()
    );

    // when
    CursorPageResponse<Article> response = articleRepository.searchArticles(request);

    // then
    assertThat(response.content())
        .extracting(Article::getTitle)
        .containsExactly("먼저 발행된 기사", "나중에 발행된 기사");
  }

  @Test
  @DisplayName("commentCount 기준 내림차순으로 뉴스 기사 목록을 정렬함")
  void searchArticles_orderByCommentCountDesc() {
    // given
    Article lowCommentArticle = Article.create(
        ArticleSource.NAVER,
        "https://news.naver.com/comment-desc-low",
        "댓글 적은 기사",
        "요약",
        LocalDateTime.of(2026, 5, 27, 10, 0)
    );

    Article highCommentArticle = Article.create(
        ArticleSource.NAVER,
        "https://news.naver.com/comment-desc-high",
        "댓글 많은 기사",
        "요약",
        LocalDateTime.of(2026, 5, 27, 11, 0)
    );

    ReflectionTestUtils.setField(lowCommentArticle, "commentCount", 1L);
    ReflectionTestUtils.setField(highCommentArticle, "commentCount", 10L);

    articleRepository.saveAllAndFlush(List.of(lowCommentArticle, highCommentArticle));

    ArticleSearchRequest request = new ArticleSearchRequest(
        null,
        null,
        null,
        null,
        null,
        "commentCount",
        "DESC",
        null,
        null,
        10,
        UUID.randomUUID()
    );

    // when
    CursorPageResponse<Article> response = articleRepository.searchArticles(request);

    // then
    assertThat(response.content())
        .extracting(Article::getTitle)
        .containsExactly("댓글 많은 기사", "댓글 적은 기사");
  }

  @Test
  @DisplayName("commentCount 기준 오름차순으로 뉴스 기사 목록을 정렬함")
  void searchArticles_orderByCommentCountAsc() {
    // given
    Article lowCommentArticle = Article.create(
        ArticleSource.NAVER,
        "https://news.naver.com/comment-asc-low",
        "댓글 적은 기사",
        "요약",
        LocalDateTime.of(2026, 5, 27, 10, 0)
    );

    Article highCommentArticle = Article.create(
        ArticleSource.NAVER,
        "https://news.naver.com/comment-asc-high",
        "댓글 많은 기사",
        "요약",
        LocalDateTime.of(2026, 5, 27, 11, 0)
    );

    ReflectionTestUtils.setField(lowCommentArticle, "commentCount", 1L);
    ReflectionTestUtils.setField(highCommentArticle, "commentCount", 10L);

    articleRepository.saveAllAndFlush(List.of(lowCommentArticle, highCommentArticle));

    ArticleSearchRequest request = new ArticleSearchRequest(
        null,
        null,
        null,
        null,
        null,
        "commentCount",
        "ASC",
        null,
        null,
        10,
        UUID.randomUUID()
    );

    // when
    CursorPageResponse<Article> response = articleRepository.searchArticles(request);

    // then
    assertThat(response.content())
        .extracting(Article::getTitle)
        .containsExactly("댓글 적은 기사", "댓글 많은 기사");
  }

  @Test
  @DisplayName("viewCount 기준 내림차순으로 뉴스 기사 목록을 정렬함")
  void searchArticles_orderByViewCountDesc() {
    // given
    Article lowViewArticle = Article.create(
        ArticleSource.NAVER,
        "https://news.naver.com/view-desc-low",
        "조회수 낮은 기사",
        "요약",
        LocalDateTime.of(2026, 5, 27, 10, 0)
    );

    Article highViewArticle = Article.create(
        ArticleSource.NAVER,
        "https://news.naver.com/view-desc-high",
        "조회수 높은 기사",
        "요약",
        LocalDateTime.of(2026, 5, 27, 11, 0)
    );

    ReflectionTestUtils.setField(lowViewArticle, "viewCount", 5L);
    ReflectionTestUtils.setField(highViewArticle, "viewCount", 30L);

    articleRepository.saveAllAndFlush(List.of(lowViewArticle, highViewArticle));

    ArticleSearchRequest request = new ArticleSearchRequest(
        null,
        null,
        null,
        null,
        null,
        "viewCount",
        "DESC",
        null,
        null,
        10,
        UUID.randomUUID()
    );

    // when
    CursorPageResponse<Article> response = articleRepository.searchArticles(request);

    // then
    assertThat(response.content())
        .extracting(Article::getTitle)
        .containsExactly("조회수 높은 기사", "조회수 낮은 기사");
  }

  @Test
  @DisplayName("viewCount 기준 오름차순으로 뉴스 기사 목록을 정렬함")
  void searchArticles_orderByViewCountAsc() {
    // given
    Article lowViewArticle = Article.create(
        ArticleSource.NAVER,
        "https://news.naver.com/view-asc-low",
        "조회수 낮은 기사",
        "요약",
        LocalDateTime.of(2026, 5, 27, 10, 0)
    );

    Article highViewArticle = Article.create(
        ArticleSource.NAVER,
        "https://news.naver.com/view-asc-high",
        "조회수 높은 기사",
        "요약",
        LocalDateTime.of(2026, 5, 27, 11, 0)
    );

    ReflectionTestUtils.setField(lowViewArticle, "viewCount", 5L);
    ReflectionTestUtils.setField(highViewArticle, "viewCount", 30L);

    articleRepository.saveAllAndFlush(List.of(lowViewArticle, highViewArticle));

    ArticleSearchRequest request = new ArticleSearchRequest(
        null,
        null,
        null,
        null,
        null,
        "viewCount",
        "ASC",
        null,
        null,
        10,
        UUID.randomUUID()
    );

    // when
    CursorPageResponse<Article> response = articleRepository.searchArticles(request);

    // then
    assertThat(response.content())
        .extracting(Article::getTitle)
        .containsExactly("조회수 낮은 기사", "조회수 높은 기사");
  }

  @Test
  @DisplayName("publishDate 커서를 사용해 다음 페이지를 조회함")
  void searchArticles_fetchNextPageByPublishDateCursor() {
    // given
    Article article1 = Article.create(
        ArticleSource.NAVER,
        "https://news.naver.com/cursor-publish-1",
        "기사 1",
        "요약 1",
        LocalDateTime.of(2026, 5, 27, 10, 0)
    );

    Article article2 = Article.create(
        ArticleSource.NAVER,
        "https://news.naver.com/cursor-publish-2",
        "기사 2",
        "요약 2",
        LocalDateTime.of(2026, 5, 27, 11, 0)
    );

    Article article3 = Article.create(
        ArticleSource.NAVER,
        "https://news.naver.com/cursor-publish-3",
        "기사 3",
        "요약 3",
        LocalDateTime.of(2026, 5, 27, 12, 0)
    );

    articleRepository.saveAllAndFlush(List.of(article1, article2, article3));

    ArticleSearchRequest firstRequest = new ArticleSearchRequest(
        null, null, null, null, null,
        "publishDate", "DESC", null, null, 2, UUID.randomUUID()
    );

    CursorPageResponse<Article> firstPage = articleRepository.searchArticles(firstRequest);

    // nextAfter 문자열 값을 파싱하여 LocalDateTime으로 바인딩함
    LocalDateTime nextAfterTime = firstPage.nextAfter() != null
        ? LocalDateTime.parse(firstPage.nextAfter(), CURSOR_DATE_FORMATTER)
        : null;

    ArticleSearchRequest secondRequest = new ArticleSearchRequest(
        null, null, null, null, null,
        "publishDate", "DESC",
        firstPage.nextCursor(),
        nextAfterTime,
        2, UUID.randomUUID()
    );

    // when
    CursorPageResponse<Article> secondPage = articleRepository.searchArticles(
        secondRequest);

    // then
    assertThat(firstPage.content())
        .extracting(Article::getTitle)
        .containsExactly("기사 3", "기사 2");

    assertThat(firstPage.hasNext()).isTrue();
    assertThat(firstPage.nextCursor()).isNotNull();
    assertThat(firstPage.nextAfter()).isNotNull();

    assertThat(secondPage.content())
        .extracting(Article::getTitle)
        .containsExactly("기사 1");

    assertThat(secondPage.hasNext()).isFalse();
  }

  @Test
  @DisplayName("commentCount 커서를 사용해 다음 페이지를 조회함")
  void searchArticles_fetchNextPageByCommentCountCursor() {
    // given
    Article article1 = Article.create(
        ArticleSource.NAVER,
        "https://news.naver.com/cursor-comment-1",
        "댓글 10개 기사",
        "요약",
        LocalDateTime.of(2026, 5, 27, 10, 0)
    );

    Article article2 = Article.create(
        ArticleSource.NAVER,
        "https://news.naver.com/cursor-comment-2",
        "댓글 20개 기사",
        "요약",
        LocalDateTime.of(2026, 5, 27, 11, 0)
    );

    Article article3 = Article.create(
        ArticleSource.NAVER,
        "https://news.naver.com/cursor-comment-3",
        "댓글 30개 기사",
        "요약",
        LocalDateTime.of(2026, 5, 27, 12, 0)
    );

    ReflectionTestUtils.setField(article1, "commentCount", 10L);
    ReflectionTestUtils.setField(article2, "commentCount", 20L);
    ReflectionTestUtils.setField(article3, "commentCount", 30L);

    articleRepository.saveAllAndFlush(List.of(article1, article2, article3));

    ArticleSearchRequest firstRequest = new ArticleSearchRequest(
        null, null, null, null, null,
        "commentCount", "DESC", null, null, 2, UUID.randomUUID()
    );

    CursorPageResponse<Article> firstPage = articleRepository.searchArticles(firstRequest);

    // nextAfter 문자열 값을 파싱하여 LocalDateTime으로 바인딩함
    LocalDateTime nextAfterTime = firstPage.nextAfter() != null
        ? LocalDateTime.parse(firstPage.nextAfter(), CURSOR_DATE_FORMATTER)
        : null;

    ArticleSearchRequest secondRequest = new ArticleSearchRequest(
        null, null, null, null, null,
        "commentCount", "DESC",
        firstPage.nextCursor(),
        nextAfterTime,
        2, UUID.randomUUID()
    );

    // when
    CursorPageResponse<Article> secondPage = articleRepository.searchArticles(
        secondRequest);

    // then
    assertThat(firstPage.content())
        .extracting(Article::getTitle)
        .containsExactly("댓글 30개 기사", "댓글 20개 기사");

    assertThat(secondPage.content())
        .extracting(Article::getTitle)
        .containsExactly("댓글 10개 기사");

    assertThat(secondPage.hasNext()).isFalse();
  }

  @Test
  @DisplayName("viewCount 커서를 사용해 다음 페이지를 조회함")
  void searchArticles_fetchNextPageByViewCountCursor() {
    // given
    Article article1 = Article.create(
        ArticleSource.NAVER,
        "https://news.naver.com/cursor-view-1",
        "조회수 10 기사",
        "요약",
        LocalDateTime.of(2026, 5, 27, 10, 0)
    );

    Article article2 = Article.create(
        ArticleSource.NAVER,
        "https://news.naver.com/cursor-view-2",
        "조회수 20 기사",
        "요약",
        LocalDateTime.of(2026, 5, 27, 11, 0)
    );

    Article article3 = Article.create(
        ArticleSource.NAVER,
        "https://news.naver.com/cursor-view-3",
        "조회수 30 기사",
        "요약",
        LocalDateTime.of(2026, 5, 27, 12, 0)
    );

    ReflectionTestUtils.setField(article1, "viewCount", 10L);
    ReflectionTestUtils.setField(article2, "viewCount", 20L);
    ReflectionTestUtils.setField(article3, "viewCount", 30L);

    articleRepository.saveAllAndFlush(List.of(article1, article2, article3));

    ArticleSearchRequest firstRequest = new ArticleSearchRequest(
        null, null, null, null, null,
        "viewCount", "DESC", null, null, 2, UUID.randomUUID()
    );

    CursorPageResponse<Article> firstPage = articleRepository.searchArticles(firstRequest);

    // nextAfter 문자열 값을 파싱하여 LocalDateTime으로 바인딩함
    LocalDateTime nextAfterTime = firstPage.nextAfter() != null
        ? LocalDateTime.parse(firstPage.nextAfter(), CURSOR_DATE_FORMATTER)
        : null;

    ArticleSearchRequest secondRequest = new ArticleSearchRequest(
        null, null, null, null, null,
        "viewCount", "DESC",
        firstPage.nextCursor(),
        nextAfterTime,
        2, UUID.randomUUID()
    );

    // when
    CursorPageResponse<Article> secondPage = articleRepository.searchArticles(
        secondRequest);

    // then
    assertThat(firstPage.content())
        .extracting(Article::getTitle)
        .containsExactly("조회수 30 기사", "조회수 20 기사");

    assertThat(secondPage.content())
        .extracting(Article::getTitle)
        .containsExactly("조회수 10 기사");

    assertThat(secondPage.hasNext()).isFalse();
  }

  @Test
  @DisplayName("limit보다 데이터가 많으면 hasNext가 true이고 limit 개수만 반환함")
  void searchArticles_hasNextByLimit() {
    // given
    Article article1 = Article.create(
        ArticleSource.NAVER,
        "https://news.naver.com/limit-1",
        "기사 1",
        "요약 1",
        LocalDateTime.of(2026, 5, 27, 10, 30)
    );

    Article article2 = Article.create(
        ArticleSource.NAVER,
        "https://news.naver.com/limit-2",
        "기사 2",
        "요약 2",
        LocalDateTime.of(2026, 5, 27, 11, 30)
    );

    Article article3 = Article.create(
        ArticleSource.NAVER,
        "https://news.naver.com/limit-3",
        "기사 3",
        "요약 3",
        LocalDateTime.of(2026, 5, 27, 12, 30)
    );

    articleRepository.saveAllAndFlush(List.of(article1, article2, article3));

    ArticleSearchRequest request = new ArticleSearchRequest(
        null,
        null,
        null,
        null,
        null,
        "publishDate",
        "DESC",
        null,
        null,
        2,
        UUID.randomUUID()
    );

    // when
    CursorPageResponse<Article> response = articleRepository.searchArticles(request);

    // then
    assertThat(response.content()).hasSize(2);
    assertThat(response.hasNext()).isTrue();
    assertThat(response.nextCursor()).isNotNull();
    assertThat(response.nextAfter()).isNotNull();
  }

  @Test
  @DisplayName("삭제되지 않은 기사만 단건 조회함")
  void findByIdAndDeletedAtIsNull_success() {
    // given
    Article article = Article.create(
        ArticleSource.NAVER,
        "https://news.naver.com/detail",
        "단건 조회 기사",
        "단건 조회 요약",
        LocalDateTime.of(2026, 5, 27, 10, 30)
    );

    Article savedArticle = articleRepository.save(article);

    // when
    Optional<Article> foundArticle = articleRepository.findByIdAndDeletedAtIsNull(
        savedArticle.getId());

    // then
    assertThat(foundArticle).isPresent();
    assertThat(foundArticle.get().getTitle()).isEqualTo("단건 조회 기사");
  }

  @TestConfiguration
  @EnableJpaAuditing(dateTimeProviderRef = "dateTimeProvider")
  static class JpaAuditingTestConfig {

    @Bean
    DateTimeProvider dateTimeProvider() {
      return () -> Optional.of(LocalDateTime.of(2026, 5, 28, 0, 0));
    }
  }
}