package com.codeit.monew.domain.comment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CommentUpdateRequest(

    @NotBlank
    @Size(min = 1, max = 500)
    String content
) { }
