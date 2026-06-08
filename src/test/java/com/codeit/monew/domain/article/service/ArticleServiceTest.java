package com.codeit.monew.domain.article.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.codeit.monew.domain.article.dto.request.ArticleSearchRequest;
import com.codeit.monew.domain.article.dto.response.ArticleDto;
import com.codeit.monew.domain.article.entity.Article;
import com.codeit.monew.domain.article.entity.ArticleSource;
import com.codeit.monew.domain.article.exception.ArticleException;
import com.codeit.monew.domain.article.repository.ArticleRepository;
import com.codeit.monew.domain.articleView.repository.ArticleViewRepository;
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
import org.springframework.test.util.ReflectionTestUtils; // 추가됨

@ExtendWith(MockitoExtension.class)
class ArticleServiceTest {

  @Mock
  private ArticleRepository articleRepository;

  @Mock
  private ArticleViewRepository articleViewRepository;

  @InjectMocks
  private ArticleService articleService;

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

    // N+1 방어용 기사 조회 여부 리포지토리 Mock 처리
    when(articleViewRepository.findViewedArticleIds(eq(requestUserId), anyList()))
        .thenReturn(Set.of()); // 빈 Set 반환 가정

    // when
    CursorPageResponse<ArticleDto> response = articleService.searchArticles(request, requestUserId);

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
    CursorPageResponse<ArticleDto> response = articleService.searchArticles(request, requestUserId);

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
}