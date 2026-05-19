package com.codeit.monew.domain.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "회원가입 정보")
public record UserRegisterRequest(
        @Schema(description = "가입 이메일")
        @NotBlank(message = "이메일은 필수 입력 값입니다.")
        @Email(message = "올바른 이메일 형식이 아닙니다.")
        String email,

        @Schema(description = "가입 닉네임", minLength = 1, maxLength = 20)
        @NotBlank(message = "닉네임은 필수 입력 값입니다.")
        @Size(min = 1, max = 20, message = "닉네임은 1자 이상 20자 이하여야 합니다.")
        String nickname,

        @Schema(description = "가입 비밀번호", minLength = 6, maxLength = 20)
        @NotBlank(message = "비밀번호는 필수 입력 값입니다.")
        @Size(min = 6, max = 20, message = "비밀번호는 6자 이상 20자 이하여야 합니다.")
        String password
) {

}
