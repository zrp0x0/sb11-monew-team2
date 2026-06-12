package com.codeit.monew.domain.article.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.codeit.monew.domain.article.entity.Article;
import com.codeit.monew.domain.article.entity.ArticleInterest;
import com.codeit.monew.domain.article.entity.ArticleSource;
import com.codeit.monew.domain.interest.entity.Interest;
import com.codeit.monew.domain.interest.repository.InterestRepository;
import com.codeit.monew.global.config.JpaAuditingConfig;
import com.codeit.monew.global.config.QuerydslConfig;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@Import({JpaAuditingConfig.class, QuerydslConfig.class})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ArticleInterestRepositoryTest {

  @Autowired
  private ArticleInterestRepository articleInterestRepository;

  @Autowired
  private ArticleRepository articleRepository;

  @Autowired
  private InterestRepository interestRepository;

  @Autowired
  private TestEntityManager tem;

  @Test
  @DisplayName("[백업용] 특정 기사에 매핑된 관심사 ID 목록을 정상적으로 조회한다.")
  void findInterestIdsByArticleId_Success() {
    // given
    Article article = articleRepository.save(Article.create(
        ArticleSource.NAVER, "url", "기사 제목", "요약", LocalDateTime.now()
    ));

    Interest interestA = interestRepository.save(Interest.create("관심사A", List.of("키워드")));
    Interest interestB = interestRepository.save(Interest.create("관심사B", List.of("키워드")));

    articleInterestRepository.save(ArticleInterest.create(article, interestA));
    articleInterestRepository.save(ArticleInterest.create(article, interestB));

    tem.flush(); tem.clear();

    // when
    List<UUID> result = articleInterestRepository.findInterestIdsByArticleId(article.getId());

    // then
    assertThat(result).hasSize(2)
        .containsExactlyInAnyOrder(interestA.getId(), interestB.getId());
  }

  @Test
  @DisplayName("[백업용] 매핑된 관심사가 없는 기사일 경우 빈 리스트를 반환한다.")
  void findInterestIdsByArticleId_Empty() {
    // given
    Article article = articleRepository.save(Article.create(
        ArticleSource.NAVER, "url", "기사 제목", "요약", LocalDateTime.now()
    ));

    tem.flush(); tem.clear();

    // when
    List<UUID> result = articleInterestRepository.findInterestIdsByArticleId(article.getId());

    // then
    assertThat(result).isNotNull().isEmpty();
  }

  @Test
  @DisplayName("[복구용] 새로운 매핑 데이터를 정상적으로 삽입하면 1을 반환한다.")
  void insertIgnoreMapping_NewMapping() {
    // given
    Article article = articleRepository.save(Article.create(
        ArticleSource.NAVER, "url", "기사 제목", "요약", LocalDateTime.now()
    ));
    Interest interest = interestRepository.save(Interest.create("관심사", List.of("키워드")));

    UUID mappingId = UUID.randomUUID();

    // when
    int insertedRows = articleInterestRepository.insertIgnoreMapping(mappingId, article.getId(), interest.getId());

    tem.flush(); tem.clear();

    // then
    assertThat(insertedRows).isEqualTo(1);
    List<UUID> mappedInterestIds = articleInterestRepository.findInterestIdsByArticleId(article.getId());
    assertThat(mappedInterestIds).hasSize(1).containsExactly(interest.getId());
  }

  @Test
  @DisplayName("[복구용] 이미 존재하는 매핑 데이터 삽입 시, 삽입이 무시되고 0을 반환한다.")
  void insertIgnoreMapping_DuplicateMapping_DoNothing() {
    // given
    Article article = articleRepository.save(Article.create(
        ArticleSource.NAVER, "url", "기사 제목", "요약", LocalDateTime.now()
    ));
    Interest interest = interestRepository.save(Interest.create("관심사", List.of("키워드")));

    // 첫 번째 삽입 (정상)
    UUID firstMappingId = UUID.randomUUID();
    articleInterestRepository.insertIgnoreMapping(firstMappingId, article.getId(), interest.getId());
    tem.flush(); tem.clear();

    // when: 동일한 articleId와 interestId로 두 번째 삽입 시도 (중복)
    UUID secondMappingId = UUID.randomUUID();
    int insertedRows = articleInterestRepository.insertIgnoreMapping(secondMappingId, article.getId(), interest.getId());

    tem.flush(); tem.clear();

    // then
    assertThat(insertedRows).isEqualTo(0);
    List<UUID> mappedInterestIds = articleInterestRepository.findInterestIdsByArticleId(article.getId());
    assertThat(mappedInterestIds).hasSize(1);
  }

  @Test
  @DisplayName("[복구용] 부모 관심사(Interest)가 DB에 존재하지 않을 경우 삽입이 무시되고 0을 반환한다.")
  void insertIgnoreMapping_InterestNotFound_DoNothing() {
    // given
    Article article = articleRepository.save(Article.create(
        ArticleSource.NAVER, "url", "기사 제목", "요약", LocalDateTime.now()
    ));

    UUID fakeInterestId = UUID.randomUUID();
    UUID mappingId = UUID.randomUUID();

    // when
    int insertedRows = articleInterestRepository.insertIgnoreMapping(mappingId, article.getId(), fakeInterestId);

    tem.flush(); tem.clear();

    // then
    assertThat(insertedRows).isEqualTo(0);
    List<UUID> mappedInterestIds = articleInterestRepository.findInterestIdsByArticleId(article.getId());
    assertThat(mappedInterestIds).isEmpty();
  }
}
