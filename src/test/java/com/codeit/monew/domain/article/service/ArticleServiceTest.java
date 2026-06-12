package com.codeit.monew.domain.article.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.codeit.monew.domain.article.dto.request.ArticleSearchRequest;
import com.codeit.monew.domain.article.dto.response.ArticleDto;
import com.codeit.monew.domain.article.entity.Article;
import com.codeit.monew.domain.article.entity.ArticleSource;
import com.codeit.monew.domain.article.exception.ArticleException;
import com.codeit.monew.domain.article.repository.ArticleRepository;
import com.codeit.monew.domain.articleView.dto.response.ArticleViewDto;
import com.codeit.monew.domain.articleView.repository.ArticleViewRepository;
import com.codeit.monew.domain.articleView.entity.ArticleView;
import com.codeit.monew.domain.user.repository.UserRepository;
import com.codeit.monew.domain.user.entity.User;
import com.codeit.monew.global.dto.CursorPageResponse;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ArticleServiceTest {

  @Mock
  private ArticleRepository articleRepository;

  @Mock
  private ArticleViewRepository articleViewRepository;

  @InjectMocks
  private ArticleService articleService;

  @Mock
  private UserRepository userRepository;

  @Test
  @DisplayName("서비스에서 지원하는 뉴스 기사 출처 목록 반환함")
  void getSources_success() {
    // when
    List<String> sources = articleService.getSources();

    // then
    assertThat(sources).containsExactly(
        "NAVER",
        "HANKYUNG",
        "CHOSUN",
        "YEONHAP"
    );
  }

  @Test
  @DisplayName("뉴스 기사 목록을 성공적으로 조회하고 DTO 페이지 응답으로 변환함")
  void searchArticles_success() {
    // given
    UUID requestUserId = UUID.randomUUID();

    ArticleSearchRequest request = new ArticleSearchRequest(
        null, null, null, null, null,
        "publishDate", "DESC", null, null, 10, requestUserId
    );

    Article article = Article.create(
        ArticleSource.NAVER,
        "https://news.naver.com/sample",
        "테스트 기사 제목",
        "테스트 기사 요약",
        LocalDateTime.of(2026, 5, 27, 10, 0)
    );

    ReflectionTestUtils.setField(article, "id", UUID.randomUUID());

    CursorPageResponse<Article> articlePage = new CursorPageResponse<>(
        List.of(article),
        "next-cursor",
        "2026-06-04T17:09:14.000000",
        1,
        1L,
        false
    );

    when(articleRepository.searchArticles(any(ArticleSearchRequest.class)))
        .thenReturn(articlePage);

    when(articleViewRepository.findViewedArticleIds(eq(requestUserId), anyList()))
        .thenReturn(Set.of());
    // when
    CursorPageResponse<ArticleDto> response = articleService.searchArticles(
        request,
        requestUserId.toString()
    );

    // then
    assertThat(response.content()).hasSize(1);
    assertThat(response.content().get(0).source()).isEqualTo(ArticleSource.NAVER);
    assertThat(response.content().get(0).title()).isEqualTo("테스트 기사 제목");
    assertThat(response.content().get(0).viewedByMe()).isFalse();

    assertThat(response.size()).isEqualTo(1);
    assertThat(response.totalElements()).isEqualTo(1L);
    assertThat(response.hasNext()).isFalse();
    assertThat(response.nextCursor()).isEqualTo("next-cursor");
    assertThat(response.nextAfter()).isEqualTo("2026-06-04T17:09:14.000000");

    verify(articleRepository).searchArticles(request);
    verify(articleViewRepository).findViewedArticleIds(eq(requestUserId), anyList());
  }

  @Test
  @DisplayName("조회된 기사 목록이 비어있으면 N+1 방어 로직을 타지 않고 바로 빈 응답을 반환함")
  void searchArticles_empty() {
    // given
    UUID requestUserId = UUID.randomUUID();
    ArticleSearchRequest request = new ArticleSearchRequest(
        null, null, null, null, null,
        "publishDate", "DESC", null, null, 10, requestUserId
    );

    CursorPageResponse<Article> emptyPage = new CursorPageResponse<>(
        Collections.emptyList(),
        null,
        null,
        10,
        0L,
        false
    );

    when(articleRepository.searchArticles(any(ArticleSearchRequest.class)))
        .thenReturn(emptyPage);

    // when
    CursorPageResponse<ArticleDto> response = articleService.searchArticles(
        request,
        requestUserId.toString()
    );

    // then
    assertThat(response.content()).isEmpty();

    verify(articleRepository).searchArticles(request);
    verifyNoInteractions(articleViewRepository);
  }


  @Test
  @DisplayName("뉴스 기사 단건 조회 성공함")
  void getArticle_success() {
    // given
    UUID articleId = UUID.randomUUID();
    UUID requestUserId = UUID.randomUUID();

    Article article = Article.create(
        ArticleSource.NAVER,
        "https://news.naver.com/sample",
        "테스트 기사 제목",
        "테스트 기사 본문",
        LocalDateTime.of(2026, 5, 27, 10, 30)
    );

    ReflectionTestUtils.setField(article, "id", articleId);

    when(articleRepository.findByIdAndDeletedAtIsNull(articleId))
        .thenReturn(Optional.of(article));

    when(articleViewRepository.findByUserIdAndArticleId(requestUserId, articleId))
            .thenReturn(Optional.empty());

    // when
    ArticleDto response = articleService.getArticle(articleId, requestUserId.toString());

    // then
    assertThat(response.source()).isEqualTo(ArticleSource.NAVER);
    assertThat(response.sourceUrl()).isEqualTo("https://news.naver.com/sample");
    assertThat(response.title()).isEqualTo("테스트 기사 제목");
    assertThat(response.summary()).isEqualTo("테스트 기사 본문");
    assertThat(response.publishDate()).isEqualTo(LocalDateTime.of(2026, 5, 27, 10, 30));
    assertThat(response.commentCount()).isEqualTo(0L);
    assertThat(response.viewCount()).isEqualTo(0L);
    assertThat(response.viewedByMe()).isFalse();
    assertThat(response.viewedByMe()).isFalse();

    verify(articleViewRepository).findByUserIdAndArticleId(requestUserId, articleId);
    verify(articleRepository).findByIdAndDeletedAtIsNull(articleId);
  }

  @Test
  @DisplayName("뉴스 기사 단건 조회시 기사 정보가 없으면 예외가 발생함")
  void getArticle_notFound() {
    // given
    UUID articleId = UUID.randomUUID();
    UUID requestUserId = UUID.randomUUID();

    when(articleRepository.findByIdAndDeletedAtIsNull(articleId))
        .thenReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() -> articleService.getArticle(articleId, requestUserId.toString()))
        .isInstanceOf(ArticleException.class);

    verify(articleRepository).findByIdAndDeletedAtIsNull(articleId);
  }

  @Test
  @DisplayName("뉴스 기사 단건 조회 시 요청자 ID 헤더가 없으면 예외가 발생함")
  void getArticle_missingRequestUserId() {
    // given
    UUID articleId = UUID.randomUUID();

    // when & then
    assertThatThrownBy(() -> articleService.getArticle(articleId, null))
        .isInstanceOf(ArticleException.class);

    verifyNoInteractions(articleRepository);
  }

  @Test
  @DisplayName("뉴스 기사 단건 조회 시 요청자 ID 형식이 올바르지 않으면 예외가 발생함")
  void getArticle_invalidRequestUserid() {
    // given
    UUID articleId = UUID.randomUUID();

    // when & then
    assertThatThrownBy(() -> articleService.getArticle(articleId, "invalid-user-id"))
        .isInstanceOf(ArticleException.class);

    verifyNoInteractions(articleRepository);
  }

  @Test
  @DisplayName("뉴스 기사 단건 조회 시 이미 조회한 기사라면 viewedByMe가 true이다")
  void getArticle_whenViewedByMeIsTrue() {
    // given
    UUID articleId = UUID.randomUUID();
    UUID requestUserId = UUID.randomUUID();

    Article article = Article.create(
            ArticleSource.NAVER,
            "https://news.naver.com/viewed",
            "조회한 기사",
            "조회한 기사 요약",
            LocalDateTime.of(2026, 5, 27, 10, 30)
    );

    ReflectionTestUtils.setField(article, "id", articleId);

    User user = User.create("viewer@example.com", "viewer", "password-hash");
    ReflectionTestUtils.setField(user, "id", requestUserId);

    ArticleView articleView = ArticleView.create(user, article);
    ReflectionTestUtils.setField(articleView, "id", UUID.randomUUID());

    when(articleRepository.findByIdAndDeletedAtIsNull(articleId))
            .thenReturn(Optional.of(article));
    when(articleViewRepository.findByUserIdAndArticleId(requestUserId, articleId))
            .thenReturn(Optional.of(articleView));

    // when
    ArticleDto response = articleService.getArticle(articleId, requestUserId.toString());

    // then
    assertThat(response.id()).isEqualTo(articleId);
    assertThat(response.viewedByMe()).isTrue();

    verify(articleRepository).findByIdAndDeletedAtIsNull(articleId);
    verify(articleViewRepository).findByUserIdAndArticleId(requestUserId, articleId);
  }

  @Test
  @DisplayName("기사 뷰 등록 시 처음 조회한 기사라면 ArticleView를 저장하고 조회 수를 증가시킨다")
  void registerArticleView_firstView_savesArticleViewAndIncreaseViewCount() {
    // given
    UUID articleId = UUID.randomUUID();
    UUID requestUserId = UUID.randomUUID();

    User user = User.create("viewer@example.com", "viewer", "password-hash");
    ReflectionTestUtils.setField(user, "id", requestUserId);

    Article article = Article.create(
            ArticleSource.NAVER,
            "https://news.naver.com/first-view",
            "처음 조회한 기사",
            "처음 조회한 기사 요약",
            LocalDateTime.of(2026, 5, 27, 10, 30)
    );
    ReflectionTestUtils.setField(article, "id", articleId);

    ArticleView savedArticleView = ArticleView.create(user, article);
    ReflectionTestUtils.setField(savedArticleView, "id", UUID.randomUUID());

    when(userRepository.findById(requestUserId))
            .thenReturn(Optional.of(user));
    when(articleRepository.findById(articleId))
            .thenReturn(Optional.of(article));
    when(articleViewRepository.findByUserIdAndArticleId(requestUserId, articleId))
            .thenReturn(Optional.empty());
    when(articleViewRepository.save(any(ArticleView.class)))
            .thenReturn(savedArticleView);

    // when
    ArticleViewDto response = articleService.registerArticleView(
            articleId,
            requestUserId.toString()
    );

    // then
    assertThat(response.articleId()).isEqualTo(articleId);
    assertThat(response.viewedBy()).isEqualTo(requestUserId);
    assertThat(article.getViewCount()).isEqualTo(1L);

    verify(userRepository).findById(requestUserId);
    verify(articleRepository).findById(articleId);
    verify(articleViewRepository).findByUserIdAndArticleId(requestUserId, articleId);
    verify(articleViewRepository).save(any(ArticleView.class));
  }

  @Test
  @DisplayName("기사 뷰 등록 시 이미 조회한 기사라면 기존 ArticleView를 반환하고 조회 수를 증가시키지 않는다")
  void registerArticleView_alreadyViewed_returnsExistingArticleViewWithoutIncreasingViewCount() {
    // given
    UUID articleId = UUID.randomUUID();
    UUID requestUserId = UUID.randomUUID();

    User user = User.create("viewer@example.com", "viewer", "password-hash");
    ReflectionTestUtils.setField(user, "id", requestUserId);

    Article article = Article.create(
            ArticleSource.NAVER,
            "https://news.naver.com/already-viewed",
            "이미 조회한 기사",
            "이미 조회한 기사 요약",
            LocalDateTime.of(2026, 5, 27, 10, 30)
    );
    ReflectionTestUtils.setField(article, "id", articleId);
    ReflectionTestUtils.setField(article, "viewCount", 1L);

    ArticleView existingArticleView = ArticleView.create(user, article);
    ReflectionTestUtils.setField(existingArticleView, "id", UUID.randomUUID());

    when(userRepository.findById(requestUserId))
            .thenReturn(Optional.of(user));
    when(articleRepository.findById(articleId))
            .thenReturn(Optional.of(article));
    when(articleViewRepository.findByUserIdAndArticleId(requestUserId, articleId))
            .thenReturn(Optional.of(existingArticleView));

    // when
    ArticleViewDto response = articleService.registerArticleView(
            articleId,
            requestUserId.toString()
    );

    // then
    assertThat(response.articleId()).isEqualTo(articleId);
    assertThat(response.viewedBy()).isEqualTo(requestUserId);
    assertThat(article.getViewCount()).isEqualTo(1L);

    verify(userRepository).findById(requestUserId);
    verify(articleRepository).findById(articleId);
    verify(articleViewRepository).findByUserIdAndArticleId(requestUserId, articleId);
    verify(articleViewRepository, never()).save(any(ArticleView.class));
  }
}
