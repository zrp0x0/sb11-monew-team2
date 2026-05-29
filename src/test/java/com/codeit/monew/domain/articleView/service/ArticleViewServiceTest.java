package com.codeit.monew.domain.articleView.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.codeit.monew.domain.article.entity.Article;
import com.codeit.monew.domain.article.entity.ArticleSource;
import com.codeit.monew.domain.article.exception.ArticleErrorCode;
import com.codeit.monew.domain.article.exception.ArticleException;
import com.codeit.monew.domain.article.repository.ArticleRepository;
import com.codeit.monew.domain.article.service.ArticleService;
import com.codeit.monew.domain.articleView.dto.response.ArticleViewDto;
import com.codeit.monew.domain.articleView.entity.ArticleView;
import com.codeit.monew.domain.articleView.repository.ArticleViewRepository;
import com.codeit.monew.domain.user.entity.User;
import com.codeit.monew.domain.user.exception.UserErrorCode;
import com.codeit.monew.domain.user.exception.UserException;
import com.codeit.monew.domain.user.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ArticleViewServiceTest {

    @Mock
    private ArticleRepository articleRepository;

    @Mock
    private ArticleViewRepository articleViewRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ArticleService articleService;

    @Test
    @DisplayName("최초 조회 시 기사 뷰를 생성하고 조회수 증가")
    void registerArticleView_firstView_createsArticleViewAndIncreasesViewCount() {
        // given
        UUID userId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID articleId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        UUID articleViewId = UUID.fromString("33333333-3333-3333-3333-333333333333");
        LocalDateTime viewedAt = LocalDateTime.of(2026, 5, 28, 12, 0);
        LocalDateTime publishedAt = LocalDateTime.of(2026, 5, 27, 10, 0);
        User user = createUser(userId);
        Article article = createArticle(articleId, publishedAt);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(articleRepository.findById(articleId)).thenReturn(Optional.of(article));
        when(articleViewRepository.findByUserIdAndArticleId(userId, articleId)).thenReturn(Optional.empty());
        when(articleViewRepository.save(any(ArticleView.class))).thenAnswer(invocation -> {
            ArticleView articleView = invocation.getArgument(0);
            ReflectionTestUtils.setField(articleView, "id", articleViewId);
            ReflectionTestUtils.setField(articleView, "createdAt", viewedAt);
            return articleView;
        });

        // when
        ArticleViewDto response = articleService.registerArticleView(articleId, userId.toString());

        // then
        assertThat(response.id()).isEqualTo(articleViewId);
        assertThat(response.viewedBy()).isEqualTo(userId);
        assertThat(response.createdAt()).isEqualTo(viewedAt);
        assertThat(response.articleId()).isEqualTo(articleId);
        assertThat(response.source()).isEqualTo(ArticleSource.NAVER);
        assertThat(response.sourceUrl()).isEqualTo("https://news.example.com/article");
        assertThat(response.articleTitle()).isEqualTo("article title");
        assertThat(response.articlePublishedDate()).isEqualTo(publishedAt);
        assertThat(response.articleSummary()).isEqualTo("article summary");
        assertThat(response.articleCommentCount()).isZero();
        assertThat(response.articleViewCount()).isEqualTo(1L);
        assertThat(article.getViewCount()).isEqualTo(1L);

        ArgumentCaptor<ArticleView> articleViewCaptor = ArgumentCaptor.forClass(ArticleView.class);
        verify(articleViewRepository).save(articleViewCaptor.capture());
        assertThat(articleViewCaptor.getValue().getUser()).isSameAs(user);
        assertThat(articleViewCaptor.getValue().getArticle()).isSameAs(article);
    }

    @Test
    @DisplayName("이미 조회한 기사면 기존 기사 뷰를 반환하고 조회수 증가 없음")
    void registerArticleView_existingView_returnsExistingArticleViewWithoutIncreasingViewCount() {
        // given
        UUID userId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID articleId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        UUID articleViewId = UUID.fromString("33333333-3333-3333-3333-333333333333");
        LocalDateTime viewedAt = LocalDateTime.of(2026, 5, 28, 12, 0);
        LocalDateTime publishedAt = LocalDateTime.of(2026, 5, 27, 10, 0);
        User user = createUser(userId);
        Article article = createArticle(articleId, publishedAt);
        article.increaseViewCount();
        ArticleView articleView = createArticleView(articleViewId, viewedAt, user, article);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(articleRepository.findById(articleId)).thenReturn(Optional.of(article));
        when(articleViewRepository.findByUserIdAndArticleId(userId, articleId)).thenReturn(Optional.of(articleView));

        // when
        ArticleViewDto response = articleService.registerArticleView(articleId, userId.toString());

        // then
        assertThat(response.id()).isEqualTo(articleViewId);
        assertThat(response.viewedBy()).isEqualTo(userId);
        assertThat(response.createdAt()).isEqualTo(viewedAt);
        assertThat(response.articleId()).isEqualTo(articleId);
        assertThat(response.articleViewCount()).isEqualTo(1L);
        assertThat(article.getViewCount()).isEqualTo(1L);

        verify(articleViewRepository, never()).save(any(ArticleView.class));
    }

    @Test
    @DisplayName("요청자 헤더가 없으면 기사 뷰 등록에 실패")
    void registerArticleView_missingRequestUserHeader_throwsUserException() {
        // given
        UUID articleId = UUID.fromString("22222222-2222-2222-2222-222222222222");

        // when & then
        assertThatThrownBy(() -> articleService.registerArticleView(articleId, null))
                .isInstanceOf(UserException.class)
                .extracting("errorCode")
                .isEqualTo(UserErrorCode.REQUEST_USER_ID_REQUIRED);

        verifyNoInteractions(userRepository, articleRepository, articleViewRepository);
    }

    @Test
    @DisplayName("요청자 헤더가 UUID 형식이 아니면 기사 뷰 등록에 실패")
    void registerArticleView_invalidRequestUserHeader_throwsUserException() {
        // given
        UUID articleId = UUID.fromString("22222222-2222-2222-2222-222222222222");

        // when & then
        assertThatThrownBy(() -> articleService.registerArticleView(articleId, "invalid-user-id"))
                .isInstanceOf(UserException.class)
                .extracting("errorCode")
                .isEqualTo(UserErrorCode.REQUEST_USER_ID_REQUIRED);

        verifyNoInteractions(userRepository, articleRepository, articleViewRepository);
    }

    @Test
    @DisplayName("존재하지 않는 사용자의 기사 뷰 등록은 실패")
    void registerArticleView_userNotFound_throwsUserException() {
        // given
        UUID userId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID articleId = UUID.fromString("22222222-2222-2222-2222-222222222222");

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> articleService.registerArticleView(articleId, userId.toString()))
                .isInstanceOf(UserException.class)
                .extracting("errorCode")
                .isEqualTo(UserErrorCode.INVALID_CREDENTIALS);

        verify(articleRepository, never()).findById(any(UUID.class));
        verify(articleViewRepository, never()).findByUserIdAndArticleId(any(UUID.class), any(UUID.class));
        verify(articleViewRepository, never()).save(any(ArticleView.class));
    }

    @Test
    @DisplayName("존재하지 않는 기사의 기사 뷰 등록은 실패")
    void registerArticleView_articleNotFound_throwsArticleException() {
        // given
        UUID userId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID articleId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        User user = createUser(userId);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(articleRepository.findById(articleId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> articleService.registerArticleView(articleId, userId.toString()))
                .isInstanceOf(ArticleException.class)
                .extracting("errorCode")
                .isEqualTo(ArticleErrorCode.ARTICLE_NOT_FOUND);

        verify(articleViewRepository, never()).findByUserIdAndArticleId(any(UUID.class), any(UUID.class));
        verify(articleViewRepository, never()).save(any(ArticleView.class));
    }

    @Test
    @DisplayName("논리삭제된 기사는 조회에서 제외되어 기사 뷰 등록에 실패")
    void registerArticleView_softDeletedArticleExcluded_throwsArticleException() {
        // given
        UUID userId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID articleId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        User user = createUser(userId);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(articleRepository.findById(articleId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> articleService.registerArticleView(articleId, userId.toString()))
                .isInstanceOf(ArticleException.class)
                .extracting("errorCode")
                .isEqualTo(ArticleErrorCode.ARTICLE_NOT_FOUND);

        verify(articleViewRepository, never()).findByUserIdAndArticleId(any(UUID.class), any(UUID.class));
        verify(articleViewRepository, never()).save(any(ArticleView.class));
    }

    private User createUser(UUID userId) {
        User user = User.create(
                "user@example.com",
                "nickname",
                "$2y$04$CnmQ.L0MoRdQxDev/JnKaOKKDqae5Ja40NMIgep0h7xRbX6jhRzZm"
        );
        ReflectionTestUtils.setField(user, "id", userId);
        return user;
    }

    private Article createArticle(UUID articleId, LocalDateTime publishedAt) {
        Article article = Article.create(
                ArticleSource.NAVER,
                "https://news.example.com/article",
                "article title",
                "article summary",
                publishedAt
        );
        ReflectionTestUtils.setField(article, "id", articleId);
        return article;
    }

    private ArticleView createArticleView(
            UUID articleViewId,
            LocalDateTime createdAt,
            User user,
            Article article
    ) {
        ArticleView articleView = ArticleView.create(user, article);
        ReflectionTestUtils.setField(articleView, "id", articleViewId);
        ReflectionTestUtils.setField(articleView, "createdAt", createdAt);
        return articleView;
    }
}
