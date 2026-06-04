package com.codeit.monew.global;

import com.codeit.monew.batch.collector.service.NewsCollectorService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/test")
public class TestController {

    private final NewsCollectorService newsCollectorService;

    @GetMapping()
    public String testGetMapping() {
        log.info("Hello World!");
        return "Hello World!";
    }

    @Operation(summary = "수동 뉴스 배치 작업", description = "수동으로 뉴스 배치 작업을 돌립니다.")
    @GetMapping("/news-batch")
    public void newsBatch() {
        newsCollectorService.collectNewsHourly();
    }
}
