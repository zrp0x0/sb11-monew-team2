package com.codeit.monew.domain.article.service;

import com.codeit.monew.domain.article.repository.ArticleRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ArticleService {

    private final ArticleRepository articleRepository;

    public List<String> getSources() {
        return articleRepository.findDistinctSources()
                .stream()
                .map(Enum::name)
                .toList();
    }
}