package com.codeit.monew.domain.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "사용자 수정 정보")
public record UserUpdateRequest(
        @Schema(description = "닉네임", minLength = 1, maxLength = 20)
        @NotBlank(message = "닉네임은 필수 입력값입니다.")
        @Size(max = 20, message = "닉네임은 20자 이하여야 합니다.")
        String nickname
) {

}
