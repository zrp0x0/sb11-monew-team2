package com.codeit.monew.domain.article.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.codeit.monew.domain.article.entity.Article;
import com.codeit.monew.domain.article.entity.ArticleSource;
import com.codeit.monew.domain.article.repository.ArticleRepository;
import com.codeit.monew.domain.interest.entity.Interest;
import com.codeit.monew.domain.interest.repository.InterestRepository;
import com.codeit.monew.external.naver.NaverNewsService;
import com.codeit.monew.external.naver.dto.NaverNewsItem;
import com.codeit.monew.external.naver.dto.NaverNewsResponse;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
class ArticleCollectServiceTest {

    @Mock
    private NaverNewsService naverNewsService;

    @Mock
    private InterestRepository interestRepository;

    @Mock
    private ArticleRepository articleRepository;

    @InjectMocks
    private ArticleCollectService articleCollectService;

    @Test
    @DisplayName("관심사 키워드로 네이버 기사를 수집하고 저장한다")
    void collectFromNaver_success() {
        // given
        Interest interest = Interest.create("인공지능", List.of(" AI ", "AI", " "));
        when(interestRepository.findAll()).thenReturn(List.of(interest));

        NaverNewsResponse response = new NaverNewsResponse(
                null,
                5,
                1,
                5,
                List.of(
                        naverItem(
                                "<b>AI</b> &amp; 뉴스",
                                "https://news.example.com/new-1",
                                "https://naver.example.com/new-1",
                                "첫 번째 <b>요약</b>",
                                "Mon, 01 Jun 2026 09:30:00 +0900"
                        ),
                        naverItem(
                                "중복 기사",
                                "https://news.example.com/new-1",
                                "https://naver.example.com/duplicate",
                                "중복 요약",
                                "Mon, 01 Jun 2026 10:30:00 +0900"
                        ),
                        naverItem(
                                "이미 저장된 기사",
                                "https://news.example.com/existing",
                                "https://naver.example.com/existing",
                                "기존 요약",
                                "Mon, 01 Jun 2026 11:30:00 +0900"
                        ),
                        naverItem(
                                "링크 대체 기사",
                                null,
                                "https://naver.example.com/fallback",
                                "링크 대체 요약",
                                "Mon, 01 Jun 2026 12:30:00 +0900"
                        )
                )
        );
        when(naverNewsService.searchNews("AI")).thenReturn(response);
        when(articleRepository.findExistingSourceUrls(anyCollection()))
                .thenReturn(Set.of("https://news.example.com/existing"));

        // when
        int savedCount = articleCollectService.collectFromNaver();

        // then
        assertThat(savedCount).isEqualTo(2);
        verify(naverNewsService).searchNews("AI");

        ArgumentCaptor<Collection<String>> sourceUrlsCaptor = ArgumentCaptor.forClass(Collection.class);
        verify(articleRepository).findExistingSourceUrls(sourceUrlsCaptor.capture());
        assertThat(sourceUrlsCaptor.getValue()).containsExactlyInAnyOrder(
                "https://news.example.com/new-1",
                "https://news.example.com/existing",
                "https://naver.example.com/fallback"
        );

        ArgumentCaptor<List<Article>> articlesCaptor = ArgumentCaptor.forClass(List.class);
        verify(articleRepository).saveAll(articlesCaptor.capture());
        List<Article> savedArticles = articlesCaptor.getValue();

        assertThat(savedArticles).hasSize(2);
        assertThat(savedArticles)
                .extracting(Article::getSourceUrl)
                .containsExactly(
                        "https://news.example.com/new-1",
                        "https://naver.example.com/fallback"
                );
        assertThat(savedArticles.get(0).getSource()).isEqualTo(ArticleSource.NAVER);
        assertThat(savedArticles.get(0).getTitle()).isEqualTo("AI & 뉴스");
        assertThat(savedArticles.get(0).getSummary()).isEqualTo("첫 번째 요약");
        assertThat(savedArticles.get(0).getPublishedAt())
                .isEqualTo(LocalDateTime.of(2026, 6, 1, 9, 30));
    }

    @Test
    @DisplayName("네이버 응답이 비어 있으면 저장하지 않는다")
    void collectFromNaver_emptyResponse() {
        // given
        Interest interest = Interest.create("경제", List.of("금리"));
        when(interestRepository.findAll()).thenReturn(List.of(interest));
        when(naverNewsService.searchNews("금리"))
                .thenReturn(new NaverNewsResponse(null, 0, 1, 0, List.of()));

        // when
        int savedCount = articleCollectService.collectFromNaver();

        // then
        assertThat(savedCount).isZero();
        verify(articleRepository, never()).findExistingSourceUrls(anyCollection());
        verify(articleRepository, never()).saveAll(anyCollection());
    }

    @Test
    @DisplayName("저장 가능한 URL이 없으면 기존 기사 조회와 저장을 하지 않는다")
    void collectFromNaver_withoutSourceUrls() {
        // given
        Interest interest = Interest.create("경제", List.of("환율"));
        when(interestRepository.findAll()).thenReturn(List.of(interest));
        when(naverNewsService.searchNews("환율"))
                .thenReturn(new NaverNewsResponse(
                        null,
                        1,
                        1,
                        1,
                        List.of(naverItem("제목", null, " ", "요약", "Mon, 01 Jun 2026 09:30:00 +0900"))
                ));

        // when
        int savedCount = articleCollectService.collectFromNaver();

        // then
        assertThat(savedCount).isZero();
        verify(articleRepository, never()).findExistingSourceUrls(anyCollection());
        verify(articleRepository, never()).saveAll(anyCollection());
    }

    @Test
    @DisplayName("같은 URL의 첫 응답이 파싱 실패해도 뒤의 정상 응답은 저장한다")
    void collectFromNaver_duplicateUrlAfterInvalidDate() {
        // given
        Interest interest = Interest.create("기술", List.of("반도체"));
        when(interestRepository.findAll()).thenReturn(List.of(interest));
        when(naverNewsService.searchNews("반도체"))
                .thenReturn(new NaverNewsResponse(
                        null,
                        2,
                        1,
                        2,
                        List.of(
                                naverItem("파싱 실패", "https://news.example.com/same", null, "요약", "invalid-date"),
                                naverItem("정상 기사", "https://news.example.com/same", null, "정상 요약", "Mon, 01 Jun 2026 09:30:00 +0900")
                        )
                ));
        when(articleRepository.findExistingSourceUrls(anyCollection())).thenReturn(Set.of());

        // when
        int savedCount = articleCollectService.collectFromNaver();

        // then
        assertThat(savedCount).isEqualTo(1);

        ArgumentCaptor<List<Article>> articlesCaptor = ArgumentCaptor.forClass(List.class);
        verify(articleRepository).saveAll(articlesCaptor.capture());
        assertThat(articlesCaptor.getValue())
                .singleElement()
                .satisfies(article -> {
                    assertThat(article.getSourceUrl()).isEqualTo("https://news.example.com/same");
                    assertThat(article.getTitle()).isEqualTo("정상 기사");
                });
    }

    @Test
    @DisplayName("관심사가 없으면 네이버 API를 호출하지 않는다")
    void collectFromNaver_withoutInterests() {
        // given
        when(interestRepository.findAll()).thenReturn(List.of());

        // when
        int savedCount = articleCollectService.collectFromNaver();

        // then
        assertThat(savedCount).isZero();
        verifyNoInteractions(naverNewsService, articleRepository);
    }

    private NaverNewsItem naverItem(
            String title,
            String originalLink,
            String link,
            String description,
            String pubDate
    ) {
        return new NaverNewsItem(title, originalLink, link, description, pubDate);
    }
}
