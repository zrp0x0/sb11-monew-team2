package com.codeit.monew.domain.article.service;

import com.codeit.monew.domain.article.repository.ArticleRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ArticleService {

    private final ArticleRepository articleRepository;

    @Transactional(readOnly = true)
    public List<String> getSources() {
        return articleRepository.findDistinctSources()
                .stream()
                .map(Enum::name)
                .toList();
    }
}