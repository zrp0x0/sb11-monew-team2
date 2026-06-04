package com.codeit.monew.domain.article.collector;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "monew.rss")
public class RssProperties {

    private boolean enabled = true;

    private Hankyung hankyung = new Hankyung();

    @Getter
    @Setter
    public static class Hankyung {

        private String url = "https://www.hankyung.com/feed/all-news";
    }
}