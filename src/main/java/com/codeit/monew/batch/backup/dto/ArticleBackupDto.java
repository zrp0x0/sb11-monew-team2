package com.codeit.monew.batch.backup.dto;

import com.codeit.monew.domain.article.entity.ArticleSource;
import java.time.LocalDateTime;
import java.util.UUID;

public record ArticleBackupDto(
    UUID id,
    ArticleSource source,
    String title,
    String summary,
    String sourceUrl,
    LocalDateTime publishDate
) {

}
