package com.codeit.monew.domain.article.service;

import com.codeit.monew.domain.article.dto.request.ArticleSearchRequest;
import com.codeit.monew.domain.article.dto.response.ArticleDto;
import com.codeit.monew.domain.article.entity.Article;
import com.codeit.monew.domain.article.entity.ArticleSource;
import com.codeit.monew.domain.article.exception.ArticleErrorCode;
import com.codeit.monew.domain.article.exception.ArticleException;
import com.codeit.monew.domain.article.repository.ArticleRepository;
import com.codeit.monew.domain.articleView.dto.response.ArticleViewDto;
import com.codeit.monew.domain.articleView.entity.ArticleView;
import com.codeit.monew.domain.articleView.repository.ArticleViewRepository;
import com.codeit.monew.domain.user.entity.User;
import com.codeit.monew.domain.user.exception.UserErrorCode;
import com.codeit.monew.domain.user.exception.UserException;
import com.codeit.monew.domain.user.repository.UserRepository;
import com.codeit.monew.global.dto.CursorPageResponse;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class ArticleService {

  private final ArticleRepository articleRepository;
  private final ArticleViewRepository articleViewRepository;
  private final UserRepository userRepository;

  public List<String> getSources() {
    return Arrays.stream(ArticleSource.values())
        .map(Enum::name)
        .toList();
  }

  @Transactional(readOnly = true)
  public CursorPageResponse<ArticleDto> searchArticles(
      ArticleSearchRequest request,
      String requestUserIdHeader) {

    UUID requestUserId = parseArticleRequestUserId(requestUserIdHeader);

    CursorPageResponse<Article> articlePage = articleRepository.searchArticles(request);

    if (articlePage.content().isEmpty()) {
      return new CursorPageResponse<>(
          Collections.emptyList(),
          articlePage.nextCursor(),
          articlePage.nextAfter(),
          articlePage.size(),
          articlePage.totalElements(),
          articlePage.hasNext()
      );
    }

    List<UUID> currentArticleIds = articlePage.content().stream()
        .map(Article::getId)
        .toList();

    Set<UUID> viewedByMes = articleViewRepository.findViewedArticleIds(
        requestUserId,
        currentArticleIds
    );

    List<ArticleDto> content = articlePage.content()
        .stream()
        .map(article -> {
          boolean viewedByMe = viewedByMes.contains(article.getId());
          return ArticleDto.from(article, viewedByMe);
        })
        .toList();

    return new CursorPageResponse<>(
        content,
        articlePage.nextCursor(),
        articlePage.nextAfter(),
        articlePage.size(),
        articlePage.totalElements(),
        articlePage.hasNext()
    );
  }

  @Transactional
  public ArticleViewDto registerArticleView(UUID articleId, String requestUserIdHeader) {
    UUID requestUserId = parseArticleViewRequestUserId(requestUserIdHeader);
    User user = userRepository.findById(requestUserId)
        .orElseThrow(() -> new UserException(UserErrorCode.INVALID_CREDENTIALS));
    Article article = articleRepository.findById(articleId)
        .orElseThrow(() -> new ArticleException(ArticleErrorCode.ARTICLE_NOT_FOUND));

    return articleViewRepository.findByUserIdAndArticleId(requestUserId, articleId)
        .map(ArticleViewDto::from)
        .orElseGet(() -> {
          ArticleView articleView = articleViewRepository.save(
              ArticleView.create(user, article));
          article.increaseViewCount();
          return ArticleViewDto.from(articleView);
        });
  }

  @Transactional(readOnly = true)
  public ArticleDto getArticle(UUID articleId, String requestUserId) {
    UUID parsedRequestUserId = parseArticleRequestUserId(requestUserId);

    Article article = articleRepository.findByIdAndDeletedAtIsNull(articleId)
            .orElseThrow(() -> new ArticleException(ArticleErrorCode.ARTICLE_NOT_FOUND));

    boolean viewedByMe = articleViewRepository.findByUserIdAndArticleId(
            parsedRequestUserId,
            articleId
    ).isPresent();

    return ArticleDto.from(article, viewedByMe);
  }

  private UUID parseArticleViewRequestUserId(String requestUserIdHeader) {
    if (!StringUtils.hasText(requestUserIdHeader)) {
      throw new UserException(UserErrorCode.REQUEST_USER_ID_REQUIRED);
    }

    try {
      return UUID.fromString(requestUserIdHeader);
    } catch (IllegalArgumentException e) {
      throw new UserException(UserErrorCode.REQUEST_USER_ID_REQUIRED);
    }
  }

  private UUID parseArticleRequestUserId(String requestUserId) {
    if (!StringUtils.hasText(requestUserId)) {
      throw new ArticleException(ArticleErrorCode.REQUEST_USER_ID_REQUIRED);
    }

    try {
      return UUID.fromString(requestUserId);
    } catch (IllegalArgumentException e) {
      throw new ArticleException(ArticleErrorCode.INVALID_REQUEST_USER_ID);
    }
  }
}
