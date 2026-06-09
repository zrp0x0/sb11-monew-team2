package com.codeit.monew.batch.collector.provider;

import com.codeit.monew.domain.article.entity.ArticleSource;
import com.codeit.monew.domain.interest.entity.Interest;
import com.codeit.monew.infra.externalapi.hankyung.client.HankyungRssClient;
import com.codeit.monew.infra.externalapi.hankyung.dto.HankyungRssItem;
import com.codeit.monew.infra.externalapi.hankyung.parser.HankyungRssParser;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.util.HtmlUtils;

@Slf4j
@Component
@RequiredArgsConstructor
public class HankyungRssNewsProvider implements NewsProvider {

    private static final DateTimeFormatter RSS_DATE_FORMATTER = DateTimeFormatter.RFC_1123_DATE_TIME;
    private static final Duration CACHE_TTL = Duration.ofMinutes(5);

    private final HankyungRssClient hankyungRssClient;
    private final HankyungRssParser hankyungRssParser;

    private volatile List<HankyungRssItem> cachedItems = Collections.emptyList();
    private volatile Instant cachedAt = Instant.EPOCH;

    @Override
    public List<CollectedNewsDto> fetchNews(Interest interest) {
        List<String> keywords = interest.getKeywords();

        if (keywords == null || keywords.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> normalizedKeywords = keywords.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .toList();

        if (normalizedKeywords.isEmpty()) {
            return Collections.emptyList();
        }

        try {
            List<HankyungRssItem> rssItems = getCachedRssItems();

            List<CollectedNewsDto> collectedNews = rssItems.stream()
                    .map(item -> toCollectedNews(item, interest, normalizedKeywords))
                    .flatMap(Optional::stream)
                    .toList();

            log.info(
                    "[news-collector] 한국경제 RSS 뉴스 후보를 수집했습니다. interestId={}, keywordCount={}, itemCount={}, matchedCount={}",
                    interest.getId(),
                    normalizedKeywords.size(),
                    rssItems.size(),
                    collectedNews.size()
            );

            return collectedNews;
        } catch (Exception e) {
            log.error(
                    "[news-collector] 한국경제 RSS 뉴스 수집 중 예외가 발생했습니다. interestId={}, errorMessage={}",
                    interest.getId(),
                    e.getMessage(),
                    e
            );
            return Collections.emptyList();
        }
    }

    @Override
    public ArticleSource getSource() {
        return ArticleSource.HANKYUNG;
    }

    private List<HankyungRssItem> getCachedRssItems() {
        Instant now = Instant.now();

        if (Duration.between(cachedAt, now).compareTo(CACHE_TTL) < 0) {
            return cachedItems;
        }

        synchronized (this) {
            now = Instant.now();

            if (Duration.between(cachedAt, now).compareTo(CACHE_TTL) < 0) {
                return cachedItems;
            }

            String rssXml = hankyungRssClient.fetchRssXml();
            List<HankyungRssItem> parsedItems = hankyungRssParser.parse(rssXml);

            cachedItems = parsedItems;
            cachedAt = now;

            log.info("[news-collector] 한국경제 RSS item {}건을 갱신했습니다.", parsedItems.size());

            return parsedItems;
        }
    }

    private Optional<CollectedNewsDto> toCollectedNews(
            HankyungRssItem item,
            Interest interest,
            List<String> keywords
    ) {
        String title = cleanText(item.title());
        String sourceUrl = normalizeUrl(item.link());
        String summary = cleanText(item.description());
        LocalDateTime publishDate = parsePubDate(item.pubDate());

        if (!StringUtils.hasText(summary)) {
            summary = title;
        }

        if (!StringUtils.hasText(title) || !StringUtils.hasText(sourceUrl)) {
            return Optional.empty();
        }

        if (!matchesKeywords(title, summary, keywords)) {
            return Optional.empty();
        }

        return Optional.of(new CollectedNewsDto(
                getSource(),
                sourceUrl,
                truncate(title, 500),
                publishDate,
                truncate(summary, 2000),
                Set.of(interest.getId())
        ));
    }

    private boolean matchesKeywords(String title, String summary, List<String> keywords) {
        String target = ((title == null ? "" : title) + " " + (summary == null ? "" : summary))
                .toLowerCase();

        return keywords.stream()
                .map(String::trim)
                .filter(StringUtils::hasText)
                .map(String::toLowerCase)
                .anyMatch(target::contains);
    }

    private LocalDateTime parsePubDate(String pubDate) {
        if (!StringUtils.hasText(pubDate)) {
            return LocalDateTime.now();
        }

        try {
            return ZonedDateTime.parse(pubDate.trim(), RSS_DATE_FORMATTER)
                    .toLocalDateTime();
        } catch (Exception e) {
            log.warn("[news-collector] 한국경제 RSS pubDate 파싱 실패. pubDate={}", pubDate);
            return LocalDateTime.now();
        }
    }

    private String cleanText(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }

        String unescaped = HtmlUtils.htmlUnescape(value);
        String removedHtml = unescaped.replaceAll("<[^>]*>", " ");

        return removedHtml
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String normalizeUrl(String url) {
        if (!StringUtils.hasText(url)) {
            return null;
        }

        String trimmedUrl = url.trim();

        try {
            URI uri = new URI(trimmedUrl);

            if (!StringUtils.hasText(uri.getHost())) {
                return trimmedUrl;
            }

            String scheme = uri.getScheme() == null ? "https" : uri.getScheme().toLowerCase();
            String userInfo = uri.getRawUserInfo();
            String host = uri.getHost().toLowerCase();
            int port = uri.getPort();
            String path = normalizePath(uri.getRawPath());
            String query = normalizeQuery(uri.getRawQuery());

            String normalizedUrl = buildNormalizedUrl(
                    scheme,
                    userInfo,
                    host,
                    port,
                    path,
                    query
            );

            return new URI(normalizedUrl).toString();
        } catch (URISyntaxException e) {
            log.warn("[news-collector] 한국경제 RSS URL 정규화 실패. 원본 URL 사용. url={}", trimmedUrl);
            return trimmedUrl;
        }
    }

    private String buildNormalizedUrl(
            String scheme,
            String userInfo,
            String host,
            int port,
            String path,
            String query
    ) {
        StringBuilder builder = new StringBuilder();

        builder.append(scheme).append("://");

        if (StringUtils.hasText(userInfo)) {
            builder.append(userInfo).append("@");
        }

        builder.append(host);

        if (port != -1) {
            builder.append(":").append(port);
        }

        builder.append(path);

        if (StringUtils.hasText(query)) {
            builder.append("?").append(query);
        }

        return builder.toString();
    }

    private String normalizePath(String path) {
        if (!StringUtils.hasText(path)) {
            return "";
        }

        if (path.length() > 1 && path.endsWith("/")) {
            return path.substring(0, path.length() - 1);
        }

        return path;
    }

    private String normalizeQuery(String query) {
        if (!StringUtils.hasText(query)) {
            return null;
        }

        List<String> filteredParams = Arrays.stream(query.split("&"))
                .filter(StringUtils::hasText)
                .filter(param -> !isTrackingParameter(param))
                .toList();

        if (filteredParams.isEmpty()) {
            return null;
        }

        return String.join("&", filteredParams);
    }

    private boolean isTrackingParameter(String param) {
        String name = param.split("=", 2)[0].toLowerCase();

        return name.startsWith("utm_")
                || name.equals("fbclid")
                || name.equals("gclid");
    }

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return "";
        }

        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
