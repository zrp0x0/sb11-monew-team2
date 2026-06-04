package com.codeit.monew.batch.collector.provider;

import com.codeit.monew.domain.article.entity.ArticleSource;
import com.codeit.monew.domain.interest.entity.Interest;
import java.util.List;

public interface NewsProvider {

    // 특정 관심사의 키워드들을 기반으로 뉴스를 수집
    List<CollectedNewsDto> fetchNews(Interest interest);

    // 어떤 Source(Naver, RSS)를 사용하는지 반환
    ArticleSource getSource();
}
