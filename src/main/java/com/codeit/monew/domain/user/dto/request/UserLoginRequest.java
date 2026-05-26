package com.codeit.monew.domain.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "로그인 정보")
public record UserLoginRequest(
        @Schema(description = "로그인 이메일")
        @NotBlank(message = "이메일은 필수 입력 값입니다.")
        @Email(message = "올바른 이메일 형식이 아닙니다.")
        String email,

        @Schema(description = "로그인 비밀번호")
        @NotBlank(message = "비밀번호는 필수 입력 값입니다.")
        String password
) {

}
