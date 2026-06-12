package com.codeit.monew.batch.collector.provider;

import com.codeit.monew.domain.article.entity.ArticleSource;
import com.codeit.monew.domain.interest.entity.Interest;
import com.codeit.monew.infra.externalapi.naver.client.NaverNewsClient;
import com.codeit.monew.infra.externalapi.naver.dto.NaverNewsResponse;
import feign.FeignException;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
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
    private static final DateTimeFormatter NAVER_DATE_FORMATTER = DateTimeFormatter.RFC_1123_DATE_TIME;

    @Value("${external-api.naver.client-id}")
    private String naverClientId;

    @Value("${external-api.naver.client-secret}")
    private String naverClientSecret;

    @Override
    public List<CollectedNewsDto> fetchNews(Interest interest) {
        List<String> keywords = interest.getKeywords();
        if (keywords == null || keywords.isEmpty()) {
            return Collections.emptyList();
        }

        String combinedQuery = keywords.stream()
            .filter(k -> k != null && !k.isBlank())
            .collect(Collectors.joining(" OR "));

        if (combinedQuery.isBlank()) {
            return Collections.emptyList();
        }

        try {
            NaverNewsResponse response = naverNewsClient.searchNews(
                naverClientId, naverClientSecret, combinedQuery, 10, 1, "date");

            if (response == null || response.items() == null) {
                log.warn("[news-collector] 네이버 뉴스 응답이 비어 있습니다. interestId={}", interest.getId());
                return Collections.emptyList();
            }

            log.info("[news-collector] 네이버 뉴스 후보를 수집했습니다. interestId={}, keywordCount={}, itemCount={}",
                interest.getId(), keywords.size(), response.items().size());

            return response.items().stream()
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
        } catch (FeignException e) {
            log.error(
                "[news-collector] 네이버 뉴스 API 호출에 실패했습니다. interestId={}, status={}, body={}",
                interest.getId(),
                e.status(),
                truncate(e.contentUTF8()),
                e
            );
            return Collections.emptyList();
        } catch (Exception e) {
            log.error("[news-collector] 네이버 뉴스 수집 중 예외가 발생했습니다. interestId={}, errorMessage={}",
                interest.getId(), e.getMessage());
            return Collections.emptyList();
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
