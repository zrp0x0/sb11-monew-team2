package com.codeit.monew.domain.article.collector;

import com.codeit.monew.domain.article.collector.HankyungRssParser.RssItem;
import com.codeit.monew.domain.article.entity.ArticleSource;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.text.StringEscapeUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
@RequiredArgsConstructor
public class RssNewsProvider implements NewsProvider {

    private final HankyungRssClient hankyungRssClient;
    private final HankyungRssParser hankyungRssParser;

    @Override
    public ArticleSource source() {
        return ArticleSource.HANKYUNG;
    }

    @Override
    public List<CollectedArticle> collect() {
        String rssXml = hankyungRssClient.fetchRssXml();
        List<RssItem> rssItems = hankyungRssParser.parse(rssXml);

        return rssItems.stream()
                .map(this::toCollectedArticle)
                .flatMap(Optional::stream)
                .toList();
    }

    private Optional<CollectedArticle> toCollectedArticle(RssItem item) {
        String title = cleanText(item.title());
        String sourceUrl = normalizeUrl(item.link());
        String summary = cleanText(item.description());
        Optional<LocalDateTime> publishedAt = parsePubDate(item.pubDate());

        if (!StringUtils.hasText(summary)) {
            summary = title;
        }

        if (!StringUtils.hasText(title)
                || !StringUtils.hasText(sourceUrl)
                || !StringUtils.hasText(summary)
                || publishedAt.isEmpty()) {
            log.warn("한국경제 RSS item 제외. title={}, sourceUrl={}, summaryExists={}, pubDate={}, parsedPublishedAt={}",
                    title,
                    sourceUrl,
                    StringUtils.hasText(summary),
                    item.pubDate(),
                    publishedAt.orElse(null));
            return Optional.empty();
        }

        return Optional.of(new CollectedArticle(
                source(),
                sourceUrl,
                truncate(title, 500),
                truncate(summary, 2000),
                publishedAt.get()
        ));
    }

    private Optional<LocalDateTime> parsePubDate(String pubDate) {
        if (!StringUtils.hasText(pubDate)) {
            return Optional.empty();
        }

        try {
            return Optional.of(
                    ZonedDateTime.parse(pubDate.trim(), DateTimeFormatter.RFC_1123_DATE_TIME)
                            .toLocalDateTime()
            );
        } catch (DateTimeParseException e) {
            log.warn("한국경제 RSS pubDate 파싱 실패. pubDate={}", pubDate);
            return Optional.empty();
        }
    }

    private String cleanText(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }

        String unescaped = StringEscapeUtils.unescapeHtml4(value);
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
            log.warn("URL 정규화 실패. 원본 URL 사용. url={}", trimmedUrl);
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
        if (value.length() <= maxLength) {
            return value;
        }

        return value.substring(0, maxLength);
    }
}