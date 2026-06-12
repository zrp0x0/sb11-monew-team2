package com.codeit.monew.batch.delete.service;

import com.codeit.monew.domain.article.repository.ArticleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ArticleHardDeleteService {

    private final ArticleRepository articleRepository;

    /**
     * 제한된 개수(batchSize)만큼의 Article과 연관 데이터를 삭제
     */
    @Transactional
    public int deleteOldArticlesBatch(LocalDateTime cutoffDate, int batchSize) {
        // 삭제할 Article ID 목록 조회
        List<UUID> articleIds = articleRepository.findOldArticleIdsWithLimit(cutoffDate, batchSize);

        // 삭제할 데이터가 없으면 0 반환
        if (articleIds.isEmpty()) {
            return 0;
        }

        // 연관 데이터 순차적 삭제
        articleRepository.hardDeleteCommentLikesByArticleIds(articleIds);
        articleRepository.hardDeleteCommentsByArticleIds(articleIds);
        articleRepository.hardDeleteArticleViewsByArticleIds(articleIds);
        articleRepository.hardDeleteArticleInterestsByArticleIds(articleIds);

        // 최종 기사 삭제
        articleRepository.hardDeleteArticlesByIds(articleIds);

        return articleIds.size();
    }
}