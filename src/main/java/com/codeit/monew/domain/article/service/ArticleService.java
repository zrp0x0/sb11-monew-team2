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

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
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
    public CursorPageResponse<ArticleDto> searchArticles(ArticleSearchRequest request) {
        validateSearchRequest(request);

        CursorPageResponse<Article> articlePage = articleRepository.searchArticles(request);

        List<ArticleDto> content = articlePage.content()
                .stream()
                .map(article -> ArticleDto.from(article, false))
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
        UUID requestUserId = parseRequestUserId(requestUserIdHeader);
        User user = userRepository.findById(requestUserId)
                .orElseThrow(() -> new UserException(UserErrorCode.INVALID_CREDENTIALS));
        Article article = articleRepository.findById(articleId)
                .orElseThrow(() -> new ArticleException(ArticleErrorCode.ARTICLE_NOT_FOUND));

        return articleViewRepository.findByUserIdAndArticleId(requestUserId, articleId)
                .map(ArticleViewDto::from)
                .orElseGet(() -> {
                    ArticleView articleView = articleViewRepository.save(ArticleView.create(user, article));
                    article.increaseViewCount();
                    return ArticleViewDto.from(articleView);
                });
    }

    private UUID parseRequestUserId(String requestUserIdHeader) {
        if (!StringUtils.hasText(requestUserIdHeader)) {
            throw new UserException(UserErrorCode.REQUEST_USER_ID_REQUIRED);
        }

        try {
            return UUID.fromString(requestUserIdHeader);
        } catch (IllegalArgumentException e) {
            throw new UserException(UserErrorCode.REQUEST_USER_ID_REQUIRED);
        }
    }

    private void validateSearchRequest(ArticleSearchRequest request) {
        validateLimit(request.limit());
        validateOrderBy(request.orderBy());
        validateDirection(request.direction());
        validateCursor(request);
    }

    private void validateLimit(int limit) {
        if (limit <= 0) {
            throw invalidSearchCondition("limit", limit);
        }
    }

    private void validateOrderBy(String orderBy) {
        if (!List.of("publishDate", "commentCount", "viewCount").contains(orderBy)) {
            throw invalidSearchCondition("orderBy", orderBy);
        }
    }

    private void validateDirection(String direction) {
        if (!List.of("ASC", "DESC").contains(direction)) {
            throw invalidSearchCondition("direction", direction);
        }
    }

    private void validateCursor(ArticleSearchRequest request) {
        if (!StringUtils.hasText(request.cursor())) {
            return;
        }

        if (request.after() == null) {
            throw invalidSearchCondition("after", null);
        }

        String[] parts = request.cursor().split("\\|", 2);

        if (parts.length != 2 || !StringUtils.hasText(parts[0]) || !StringUtils.hasText(parts[1])) {
            throw invalidSearchCondition("cursor", request.cursor());
        }

        try {
            UUID.fromString(parts[1]);

            switch (request.orderBy()) {
                case "publishDate" -> LocalDateTime.parse(parts[0]);
                case "commentCount", "viewCount" -> Long.valueOf(parts[0]);
                default -> throw invalidSearchCondition("orderBy", request.orderBy());
            }
        } catch (IllegalArgumentException | DateTimeParseException e) {
            throw invalidSearchCondition("cursor", request.cursor());
        }
    }

    private ArticleException invalidSearchCondition(String field, Object value) {
        return new ArticleException(
                ArticleErrorCode.INVALID_ARTICLE_SEARCH_CONDITION,
                Map.of(field, String.valueOf(value))
        );
    }
}

    @Transactional(readOnly = true)
    public ArticleDto getArticle(UUID articleId, String requestUserId) {
        UUID parsedRequestUserId = parseRequestUserId(requestUserId);

        Article article = articleRepository.findByIdAndDeletedAtIsNull(articleId)
                .orElseThrow(() -> new ArticleException(ArticleErrorCode.ARTICLE_NOT_FOUND));

        // Todo: viewedByMe false로 두고 추후 고도화
        return ArticleDto.from(article, false);
    }

    private UUID parseRequestUserId(String requestUserId) {
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
