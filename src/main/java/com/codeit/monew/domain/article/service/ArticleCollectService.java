package com.codeit.monew.domain.article.service;

import com.codeit.monew.domain.article.entity.Article;
import com.codeit.monew.domain.article.entity.ArticleSource;
import com.codeit.monew.domain.article.repository.ArticleRepository;
import com.codeit.monew.domain.interest.entity.Interest;
import com.codeit.monew.domain.interest.repository.InterestRepository;
import com.codeit.monew.external.naver.NaverNewsService;
import com.codeit.monew.external.naver.dto.NaverNewsItem;
import com.codeit.monew.external.naver.dto.NaverNewsResponse;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.text.StringEscapeUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ArticleCollectService {

    private final NaverNewsService naverNewsService;
    private final InterestRepository interestRepository;
    private final ArticleRepository articleRepository;

    public int collectFromNaver() {
        Set<String> keywords = interestRepository.findAll().stream()
                .map(Interest::getKeywords)
                .filter(Objects::nonNull)
                .flatMap(List::stream)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(keyword -> !keyword.isBlank())
                .collect(Collectors.toCollection(LinkedHashSet::new));

        return keywords.stream()
                .mapToInt(this::collectFromNaverByKeyword)
                .sum();
    }

    private int collectFromNaverByKeyword(String keyword) {
        NaverNewsResponse response = naverNewsService.searchNews(keyword);

        if (response == null || response.items() == null || response.items().isEmpty()) {
            return 0;
        }

        Set<String> sourceUrls = response.items().stream()
                .map(this::resolveSourceUrl)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (sourceUrls.isEmpty()) {
            return 0;
        }

        Set<String> existingUrls = articleRepository.findExistingSourceUrls(sourceUrls);

        Set<String> seenUrls = new HashSet<>();

        List<Article> articles = response.items().stream()
                .filter(item -> {
                    String sourceUrl = resolveSourceUrl(item);
                    return sourceUrl != null
                            && !existingUrls.contains(sourceUrl)
                            && seenUrls.add(sourceUrl);
                })
                .map(item -> toArticle(item, existingUrls))
                .flatMap(Optional::stream)
                .toList();

        articleRepository.saveAll(articles);

        log.info("네이버 기사 수집 완료. keyword={}, savedCount={}", keyword, articles.size());

        return articles.size();
    }

    private Optional<Article> toArticle(NaverNewsItem item, Set<String> existingUrls) {
        String sourceUrl = resolveSourceUrl(item);
        if (sourceUrl == null || existingUrls.contains(sourceUrl)) {
            return Optional.empty();
        }

        return parsePublishedAt(item.pubDate())
                .map(publishedAt -> Article.create(
                    ArticleSource.NAVER,
                    sourceUrl,
                    normalizeText(item.title(), 500),
                    normalizeText(item.description(), 2000),
                    publishedAt
                ));
    }

    private String resolveSourceUrl(NaverNewsItem item) {
        if (item.originallink() != null && !item.originallink().isBlank()) {
            return item.originallink().trim();
        }

        if (item.link() != null && !item.link().isBlank()) {
            return item.link().trim();
        }

        return null;
    }

    private Optional<LocalDateTime> parsePublishedAt(String pubDate) {
        if (pubDate == null || pubDate.isBlank()) {
            return Optional.empty();
        }

        try {
            return Optional.of(ZonedDateTime
                    .parse(pubDate, DateTimeFormatter.RFC_1123_DATE_TIME)
                    .toLocalDateTime());
        } catch (DateTimeParseException e) {
            log.warn("네이버 기사 발행일 파싱 실패. pubDate={}", pubDate, e);
            return Optional.empty();
        }
    }

    private String normalizeText(String value, int maxLength) {
        String normalized = StringEscapeUtils.unescapeHtml4(value == null ? "" : value)
                .replaceAll("<[^>]*>", "")
                .trim();

        if (normalized.length() <= maxLength) {
            return normalized;
        }
        return normalized.substring(0, maxLength);
    }
}
