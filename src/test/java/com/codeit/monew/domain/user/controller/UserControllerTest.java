package com.codeit.monew.domain.user.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.codeit.monew.domain.user.dto.request.UserRegisterRequest;
import com.codeit.monew.domain.user.dto.response.UserDto;
import com.codeit.monew.domain.user.exception.UserErrorCode;
import com.codeit.monew.domain.user.exception.UserException;
import com.codeit.monew.domain.user.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    UserService userService;

    @Test
    @DisplayName("회원가입 성공 시 201 Created와 UserDto를 반환")
    void register_success() throws Exception {
        // given
        UUID id = UUID.fromString("11111111-1111-1111-1111-111111111111");
        LocalDateTime createdAt = LocalDateTime.of(2026, 5, 26, 12, 0);
        UserDto response = new UserDto(id, "user@example.com", "User", createdAt);
        Map<String, String> request = Map.of(
                "email", "user@example.com",
                "nickname", "User",
                "password", "password"
        );

        when(userService.register(any(UserRegisterRequest.class))).thenReturn(response);

        // when & then
        mockMvc.perform(post("/api/users")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.email").value("user@example.com"))
                .andExpect(jsonPath("$.nickname").value("User"))
                .andExpect(jsonPath("$.createdAt").value("2026-05-26T12:00:00"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @ParameterizedTest(name = "{0}") // 같은 테스트 로직을 반복 실행
    @MethodSource("invalidRegisterRequests")
    @DisplayName("회원가입 입력값이 유효하지 않으면 400 Bad Request를 반환")
    void register_fail_whenRequestInvalid(String description, Map<String, String> request, String invalidField)
            throws Exception {
        // when & then
        mockMvc.perform(post("/api/users")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"))
                .andExpect(jsonPath("$.details." + invalidField).exists());
    }

    @Test
    @DisplayName("중복 이메일이면 409 Conflict를 반환")
    void register_fail_whenEmailDuplicated() throws Exception {
        // given
        Map<String, String> request = Map.of(
                "email", "user@example.com",
                "nickname", "User",
                "password", "password"
        );

        when(userService.register(any(UserRegisterRequest.class)))
                .thenThrow(new UserException(UserErrorCode.EMAIL_DUPLICATION));

        // when & then
        mockMvc.perform(post("/api/users")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.code").value("EMAIL_DUPLICATION"))
                .andExpect(jsonPath("$.message").value("이미 존재하는 이메일입니다."))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    private static Stream<Arguments> invalidRegisterRequests() {
        return Stream.of(
                Arguments.of("이메일 공백", registerRequest("", "User", "password"), "email"),
                Arguments.of("이메일 형식 오류", registerRequest("not-email", "User", "password"), "email"),
                Arguments.of("닉네임 공백", registerRequest("user@example.com", "", "password"), "nickname"),
                Arguments.of("닉네임 20자 초과", registerRequest("user@example.com", "a".repeat(21), "password"), "nickname"),
                Arguments.of("비밀번호 6자 미만", registerRequest("user@example.com", "User", "12345"), "password"),
                Arguments.of("비밀번호 20자 초과", registerRequest("user@example.com", "User", "a".repeat(21)), "password")
        );
    }

    private static Map<String, String> registerRequest(String email, String nickname, String password) {
        return Map.of(
                "email", email,
                "nickname", nickname,
                "password", password
        );
    }
}
