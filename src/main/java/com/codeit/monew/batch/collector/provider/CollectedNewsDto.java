package com.codeit.monew.batch.collector.provider;

import com.codeit.monew.domain.article.entity.ArticleSource;
import java.time.LocalDateTime;
import java.util.UUID;

public record CollectedNewsDto(
    ArticleSource source,
    String sourceUrl,
    String title,
    LocalDateTime publishDate,
    String summary,
    UUID interestId
) {

}
