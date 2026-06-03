package com.codeit.monew.domain.article.collector;

import com.codeit.monew.domain.article.entity.Article;
import com.codeit.monew.domain.article.entity.ArticleSource;
import com.codeit.monew.domain.article.repository.ArticleRepository;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import javax.xml.parsers.DocumentBuilderFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.text.StringEscapeUtils;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.parameters.P;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

@Slf4j
@Service
@RequiredArgsConstructor
public class RssArticleCollector {

    private static final String HANKYUNG_RSS_URL = "https://www.hankyung.com/feed/all-news";

    private final ArticleRepository articleRepository;
    private final RestTemplateBuilder restTemplateBuilder;

    public CollectResult collect() {
        String rssXml = fetchRssXml();
        List<RssItem> rssItems = parseRssItems(rssXml);

        int savedCount = 0;
        int skippedCount = 0;
        int failedCount = 0;

        for (RssItem item : rssItems) {
            try {
                Optional<Article> articleOptional = toArticle(item);

                if (articleOptional.isEmpty()) {
                    skippedCount++;
                    continue;
                }

                Article article = articleOptional.get();

                if (articleRepository.existsBySourceUrlIncludingDeleted(article.getSourceUrl())) {
                    skippedCount++;
                    continue;
                }

                articleRepository.save(article);
                savedCount++;
            } catch (DataIntegrityViolationException e) {
                skippedCount++;
                log.info("한국경제 RSS 중복 기사 저장 skip. link={}", item.link());
            } catch (Exception e) {
                failedCount++;
                log.warn("한국경제 RSS 기사 저장 실패. title={}, link={}", item.title(), item.link(), e);
            }
        }

        return new CollectResult(
                rssItems.size(),
                savedCount,
                skippedCount,
                failedCount
        );
    }

    private String fetchRssXml() {
        try {
            RestTemplate restTemplate = restTemplateBuilder.build();
            String rssXml = restTemplate.getForObject(HANKYUNG_RSS_URL, String.class);

            if (!StringUtils.hasText(rssXml)) {
                throw new IllegalArgumentException("한국경제 RSS 응답이 비어 있습니다.");
            }

            return rssXml;
        } catch (RestClientException e) {
            throw new IllegalStateException("한국경제 RSS 호출에 실패했습니다.", e);
        }
    }

    private List<RssItem> parseRssItems(String rssXml) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);

            Document document = factory.newDocumentBuilder()
                    .parse(new InputSource(new StringReader(rssXml)));

            NodeList itemNodes = document.getElementsByTagName("item");

            return java.util.stream.IntStream.range(0, itemNodes.getLength())
                    .mapToObj(itemNodes::item)
                    .filter(node -> node instanceof Element)
                    .map(node -> (Element) node)
                    .map(itemElement -> new RssItem(
                            getTextContent(itemElement, "title"),
                            getTextContent(itemElement, "link"),
                            getTextContent(itemElement, "description"),
                            getTextContent(itemElement, "pubDate")
                    ))
                    .toList();
        } catch (Exception e) {
            throw new IllegalArgumentException("한국경제 RSS 파싱에 실패했습니다.", e);
        }
    }

    private String getTextContent(Element element, String tagName) {
        NodeList nodeList = element.getElementsByTagName(tagName);

        if (nodeList.getLength() == 0) {
            return null;
        }

        return nodeList.item(0).getTextContent();
    }

    private Optional<Article> toArticle(RssItem item) {
        String title = cleanText(item.title());
        String sourceUrl = normalizeUrl(item.link());
        String summary = cleanText(item.description());
        Optional<LocalDateTime> publishedAt = parsePubDate(item.pubDate());

        if (!StringUtils.hasText(title)
            || !StringUtils.hasText(sourceUrl)
            || !StringUtils.hasText(summary)
            || publishedAt.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(Article.create(
                ArticleSource.HANKYUNG,
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

    private record RssItem(
            String title,
            String link,
            String description,
            String pubDate
    ) {
    }

    public record CollectResult(
            int totalCount,
            int savedCount,
            int skippedCount,
            int failedCount
    ) {
    }
}
