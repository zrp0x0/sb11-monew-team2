package com.codeit.monew.batch.collector.provider;

import com.codeit.monew.domain.article.entity.ArticleSource;
import com.codeit.monew.domain.interest.entity.Interest;
import com.codeit.monew.global.monitoring.service.MonewMetrics;
import com.codeit.monew.infra.externalapi.naver.client.NaverNewsClient;
import com.codeit.monew.infra.externalapi.naver.dto.NaverNewsResponse;
import feign.FeignException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

@Slf4j
@Component
@RequiredArgsConstructor
public class NaverNewsProvider implements NewsProvider {

    private final NaverNewsClient naverNewsClient;
    private final MonewMetrics monewMetrics;
    private static final DateTimeFormatter NAVER_DATE_FORMATTER = DateTimeFormatter.RFC_1123_DATE_TIME;

    @Value("${external-api.naver.client-id}")
    private String naverClientId;

    @Value("${external-api.naver.client-secret}")
    private String naverClientSecret;

    @Override
    public NewsFetchResult fetchNews(Interest interest) {
        List<String> keywords = interest.getKeywords();
        if (keywords == null || keywords.isEmpty()) {
            return NewsFetchResult.skipped(getSource(), "No keywords");
        }

        String combinedQuery = keywords.stream()
            .filter(k -> k != null && !k.isBlank())
            .collect(Collectors.joining(" OR "));

        if (combinedQuery.isBlank()) {
            return NewsFetchResult.skipped(getSource(), "No valid keywords");
        }

        long startedAt = System.nanoTime();
        monewMetrics.incrementNaverCalls();
        try {
            NaverNewsResponse response = naverNewsClient.searchNews(
                naverClientId, naverClientSecret, combinedQuery, 10, 1, "date");

            if (response == null || response.items() == null || response.items().isEmpty()) {
                log.warn("[news-collector] 네이버 뉴스 응답이 비어 있습니다. interestId={}", interest.getId());
                monewMetrics.incrementNaverEmptyResponses();
                return NewsFetchResult.empty(getSource(), "Empty response");
            }

            log.info("[news-collector] 네이버 뉴스 후보를 수집했습니다. interestId={}, keywordCount={}, itemCount={}",
                interest.getId(), keywords.size(), response.items().size());

            List<CollectedNewsDto> collectedNews = response.items().stream()
                .filter(item -> (item.originallink() != null && !item.originallink().isBlank())
                    || (item.link() != null && !item.link().isBlank()))
                .map(item -> {
                    String targetUrl =
                        (item.originallink() != null && !item.originallink().isBlank())
                            ? item.originallink() : item.link();

                    return new CollectedNewsDto(
                        getSource(),
                        targetUrl,
                        stripHtmlTags(item.title()),
                        parsePubDate(item.pubDate()),
                        stripHtmlTags(item.description()),
                        Set.of(interest.getId())
                    );
                })
                .toList();

            if (collectedNews.isEmpty()) {
                monewMetrics.incrementNaverEmptyResponses();
                return NewsFetchResult.empty(getSource(), "No valid article URLs");
            }

            return NewsFetchResult.success(getSource(), collectedNews);
        } catch (FeignException e) {
            log.error(
                "[news-collector] 네이버 뉴스 API 호출에 실패했습니다. interestId={}, status={}, body={}",
                interest.getId(),
                e.status(),
                truncate(e.contentUTF8()),
                e
            );
            monewMetrics.incrementNaverErrors();
            return NewsFetchResult.failed(getSource(), "Naver API failure: status=" + e.status());
        } catch (Exception e) {
            log.error("[news-collector] 네이버 뉴스 수집 중 예외가 발생했습니다. interestId={}, errorMessage={}",
                interest.getId(), e.getMessage(), e);
            monewMetrics.incrementNaverErrors();
            return NewsFetchResult.failed(getSource(), e.getMessage());
        } finally {
            monewMetrics.recordNaverDuration(Duration.ofNanos(System.nanoTime() - startedAt));
        }
    }

    @Override
    public ArticleSource getSource() {
        return ArticleSource.NAVER;
    }

    private String stripHtmlTags(String text) {
        if (text == null) {
            return "";
        }
        String noTagText = text.replaceAll("<[^>]*>", "");
        return HtmlUtils.htmlUnescape(noTagText);
    }

    private LocalDateTime parsePubDate(String pubDateStr) {
        try {
            ZonedDateTime zonedDateTime = ZonedDateTime.parse(pubDateStr, NAVER_DATE_FORMATTER);
            return zonedDateTime.toLocalDateTime();
        } catch (Exception e) {
            return LocalDateTime.now();
        }
    }

    private String truncate(String value) {
        if (value == null) {
            return "";
        }
        int maxLength = 500;
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
