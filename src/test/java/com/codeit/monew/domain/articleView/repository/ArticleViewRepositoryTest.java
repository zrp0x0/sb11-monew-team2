package com.codeit.monew.domain.articleView.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.codeit.monew.domain.article.entity.Article;
import com.codeit.monew.domain.article.entity.ArticleSource;
import com.codeit.monew.domain.articleView.entity.ArticleView;
import com.codeit.monew.domain.user.entity.User;
import com.codeit.monew.global.config.QueryDslTestConfig;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@Import(QueryDslTestConfig.class)
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ArticleViewRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ArticleViewRepository articleViewRepository;

    @Test
    @DisplayName("userId와 articleId 조건으로 ArticleView 조회 시 불필요한 join이 발생하지 않음")
    void findByUserIdAndArticleId_returnsArticleViewWithJpql() {
        // given
        User user = entityManager.persistAndFlush(
                User.create("viewer@example.com", "viewer", "password-hash")
        );
        Article article = entityManager.persistAndFlush(
                Article.create(
                        ArticleSource.NAVER,
                        "https://news.example.com/article-view-repository-test",
                        "title",
                        "summary",
                        LocalDateTime.of(2026, 5, 29, 12, 0)
                )
        );
        ArticleView articleView = entityManager.persistAndFlush(ArticleView.create(user, article));
        entityManager.clear();

        // when
        Optional<ArticleView> result = articleViewRepository.findByUserIdAndArticleId(
                user.getId(),
                article.getId()
        );

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(articleView.getId());
        assertThat(result.get().getUser().getId()).isEqualTo(user.getId());
        assertThat(result.get().getArticle().getId()).isEqualTo(article.getId());
    }
}
