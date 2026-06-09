package com.codeit.monew.domain.article.collector;

import com.codeit.monew.domain.article.entity.Article;
import com.codeit.monew.domain.article.repository.ArticleRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Profile("dev")
@Service
@RequiredArgsConstructor
public class ManualRssCollectService {

    private final RssNewsProvider rssNewsProvider;
    private final ArticleRepository articleRepository;

    @Transactional
    public ManualRssCollectResult collectHankyungRss() {
        List<CollectedArticle> collectedArticles = rssNewsProvider.collect();

        int savedCount = 0;
        int skippedCount = 0;

        for (CollectedArticle collectedArticle : collectedArticles) {
            Article article = Article.restore(
                    UUID.randomUUID(),
                    collectedArticle.source(),
                    collectedArticle.sourceUrl(),
                    collectedArticle.title(),
                    collectedArticle.summary(),
                    collectedArticle.publishedAt()
            );

            int affectedRows = articleRepository.upsertArticleSkipDuplicate(article);

            if (affectedRows > 0) {
                savedCount++;
            } else {
                skippedCount++;
            }
        }

        return new ManualRssCollectResult(
                collectedArticles.size(),
                savedCount,
                skippedCount
        );
    }

    public record ManualRssCollectResult(
            int collectedCount,
            int savedCount,
            int skippedCount
    ) {
    }
}