package com.codeit.monew.domain.article.service;

import com.codeit.monew.domain.article.entity.ArticleSource;
import java.util.Arrays;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ArticleService {

    public List<String> getSources() {
        return Arrays.stream(ArticleSource.values())
                .map(Enum::name)
                .toList();
    }
}