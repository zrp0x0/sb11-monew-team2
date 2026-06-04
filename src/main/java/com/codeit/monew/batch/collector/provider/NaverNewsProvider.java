package com.codeit.monew.batch.collector.provider;

import com.codeit.monew.domain.article.entity.ArticleSource;
import com.codeit.monew.domain.interest.entity.Interest;
import com.codeit.monew.infra.externalapi.naver.client.NaverNewsClient;
import com.codeit.monew.infra.externalapi.naver.dto.NaverNewsResponse;
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
                return Collections.emptyList();
            }

            return response.items().stream()
                // 원문 링크(originallink)나 네이버 링크(link) 중 하나라도 있으면 통과
                .filter(item -> (item.originallink() != null && !item.originallink().isBlank())
                    || (item.link() != null && !item.link().isBlank()))
                .map(item -> {
                    // Fallback 적용: originallink가 유효하면 우선 사용, 없으면 link 사용 - PR 반영
                    String targetUrl =
                        (item.originallink() != null && !item.originallink().isBlank())
                            ? item.originallink() : item.link();

                    return new CollectedNewsDto(
                        getSource(),
                        targetUrl,
                        stripHtmlTags(item.title()),
                        parsePubDate(item.pubDate()),
                        stripHtmlTags(item.description()),
                        Set.of(interest.getId()) // Set으로 감싸서 전달
                    );
                })
                .toList();
        } catch (Exception e) {
            log.error("[NaverNewsProvider] 뉴스 수집 중 예외 발생. 관심사 ID: {}, 원인: {}", interest.getId(),
                e.getMessage());
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
}