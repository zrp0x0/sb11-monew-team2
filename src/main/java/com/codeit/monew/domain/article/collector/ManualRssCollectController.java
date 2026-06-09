package com.codeit.monew.domain.article.collector;

import com.codeit.monew.domain.article.collector.ManualRssCollectService.ManualRssCollectResult;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@Profile("dev")
@RestController
@RequiredArgsConstructor
public class ManualRssCollectController {

    private final ManualRssCollectService manualRssCollectService;

    @PostMapping("/api/dev/articles/rss/collect")
    public ManualRssCollectResult collectHankyungRss() {
        return manualRssCollectService.collectHankyungRss();
    }
}