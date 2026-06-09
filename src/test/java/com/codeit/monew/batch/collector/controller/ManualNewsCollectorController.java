package com.codeit.monew.batch.collector.controller;

import com.codeit.monew.batch.collector.service.NewsCollectorService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@Profile("dev")
@RestController
@RequiredArgsConstructor
public class ManualNewsCollectorController {

    private final NewsCollectorService newsCollectorService;

    @PostMapping("/api/dev/news-collector/run")
    public String runNewsCollector() {
        newsCollectorService.collectNewsHourly();
        return "OK";
    }
}