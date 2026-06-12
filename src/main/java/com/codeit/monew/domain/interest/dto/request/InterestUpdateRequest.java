package com.codeit.monew.domain.interest.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.hibernate.validator.constraints.UniqueElements;

public record InterestUpdateRequest(
    @NotNull(message = "키워드 목록은 필수입니다.")
    @Size(min = 1, max = 10, message = "키워드는 1개 이상 10개 이하로 등록해야 합니다.")
    @UniqueElements(message = "중복된 키워드는 입력할 수 없습니다.")
    List<
        @NotBlank(message = "빈 키워드는 허용되지 않습니다.")
        @Pattern(
            regexp = "^[가-힣a-zA-Z0-9]+$",
            message = "키워드는 완성된 한글, 영문, 숫자만 사용할 수 있습니다."
        )
        String> keywords
) {
}
