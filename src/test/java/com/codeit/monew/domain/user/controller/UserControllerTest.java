package com.codeit.monew.domain.user.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.codeit.monew.domain.user.dto.request.UserLoginRequest;
import com.codeit.monew.domain.user.dto.request.UserRegisterRequest;
import com.codeit.monew.domain.user.dto.request.UserUpdateRequest;
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

    @Test
    @DisplayName("로그인 성공 시 200 OK와 UserDto를 반환")
    void login_success() throws Exception {
        // given
        UUID id = UUID.fromString("11111111-1111-1111-1111-111111111111");
        LocalDateTime createdAt = LocalDateTime.of(2026, 5, 26, 12, 0);
        UserDto response = new UserDto(id, "user@example.com", "User", createdAt);
        Map<String, String> request = Map.of(
                "email", "user@example.com",
                "password", "password"
        );

        when(userService.login(any(UserLoginRequest.class))).thenReturn(response);

        // when & then
        mockMvc.perform(post("/api/users/login")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.email").value("user@example.com"))
                .andExpect(jsonPath("$.nickname").value("User"))
                .andExpect(jsonPath("$.createdAt").value("2026-05-26T12:00:00"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidLoginRequests")
    @DisplayName("로그인 입력값이 유효하지 않으면 400 Bad Request를 반환")
    void login_fail_whenRequestInvalid(String description, Map<String, String> request, String invalidField)
            throws Exception {
        // when & then
        mockMvc.perform(post("/api/users/login")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"))
                .andExpect(jsonPath("$.details." + invalidField).exists());
    }

    @Test
    @DisplayName("로그인 인증 실패 시 401 Unauthorized를 반환")
    void login_fail_whenCredentialsInvalid() throws Exception {
        // given
        Map<String, String> request = Map.of(
                "email", "user@example.com",
                "password", "password"
        );

        when(userService.login(any(UserLoginRequest.class)))
                .thenThrow(new UserException(UserErrorCode.INVALID_CREDENTIALS));

        // when & then
        mockMvc.perform(post("/api/users/login")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.message").value("이메일 또는 비밀번호가 올바르지 않습니다."))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    @DisplayName("사용자 정보 수정 성공 시 200 OK와 변경된 UserDto를 반환")
    void update_success() throws Exception {
        // given
        UUID userId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        LocalDateTime createdAt = LocalDateTime.of(2026, 5, 26, 12, 0);
        UserDto response = new UserDto(userId, "user@example.com", "NewNickname", createdAt);
        Map<String, String> request = Map.of("nickname", "NewNickname");

        when(userService.update(eq(userId), eq(userId.toString()), any(UserUpdateRequest.class)))
                .thenReturn(response);

        // when & then
        mockMvc.perform(patch("/api/users/{userId}", userId)
                        .header("Monew-Request-User-ID", userId.toString())
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(userId.toString()))
                .andExpect(jsonPath("$.email").value("user@example.com"))
                .andExpect(jsonPath("$.nickname").value("NewNickname"))
                .andExpect(jsonPath("$.createdAt").value("2026-05-26T12:00:00"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidUpdateRequests")
    @DisplayName("사용자 정보 수정 입력값이 유효하지 않으면 400 Bad Request를 반환")
    void update_fail_whenRequestInvalid(String description, Map<String, String> request)
            throws Exception {
        // given
        UUID userId = UUID.fromString("11111111-1111-1111-1111-111111111111");

        // when & then
        mockMvc.perform(patch("/api/users/{userId}", userId)
                        .header("Monew-Request-User-ID", userId.toString())
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"))
                .andExpect(jsonPath("$.details.nickname").exists());
    }

    @Test
    @DisplayName("요청자 헤더가 없으면 401 Unauthorized를 반환")
    void update_fail_whenRequestUserHeaderMissing() throws Exception {
        // given
        UUID userId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        Map<String, String> request = Map.of("nickname", "NewNickname");

        when(userService.update(eq(userId), isNull(), any(UserUpdateRequest.class)))
                .thenThrow(new UserException(UserErrorCode.REQUEST_USER_ID_REQUIRED));

        // when & then
        mockMvc.perform(patch("/api/users/{userId}", userId)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("REQUEST_USER_ID_REQUIRED"));
    }

    @Test
    @DisplayName("요청자 ID와 수정 대상 userId가 다르면 403 Forbidden을 반환")
    void update_fail_whenRequestUserMismatched() throws Exception {
        // given
        UUID userId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID requestUserId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        Map<String, String> request = Map.of("nickname", "NewNickname");

        when(userService.update(eq(userId), eq(requestUserId.toString()), any(UserUpdateRequest.class)))
                .thenThrow(new UserException(UserErrorCode.USER_ACCESS_DENIED));

        // when & then
        mockMvc.perform(patch("/api/users/{userId}", userId)
                        .header("Monew-Request-User-ID", requestUserId.toString())
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.code").value("USER_ACCESS_DENIED"));
    }

    @Test
    @DisplayName("수정 대상 사용자가 없으면 404 Not Found를 반환")
    void update_fail_whenUserNotFound() throws Exception {
        // given
        UUID userId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        Map<String, String> request = Map.of("nickname", "NewNickname");

        when(userService.update(eq(userId), eq(userId.toString()), any(UserUpdateRequest.class)))
                .thenThrow(new UserException(UserErrorCode.USER_NOT_FOUND));

        // when & then
        mockMvc.perform(patch("/api/users/{userId}", userId)
                        .header("Monew-Request-User-ID", userId.toString())
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"));
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

    private static Stream<Arguments> invalidLoginRequests() {
        return Stream.of(
                Arguments.of("이메일 공백", loginRequest("", "password"), "email"),
                Arguments.of("이메일 형식 오류", loginRequest("not-email", "password"), "email"),
                Arguments.of("비밀번호 공백", loginRequest("user@example.com", ""), "password")
        );
    }

    private static Stream<Arguments> invalidUpdateRequests() {
        return Stream.of(
                Arguments.of("닉네임 공백", updateRequest("")),
                Arguments.of("닉네임 20자 초과", updateRequest("a".repeat(21)))
        );
    }

    private static Map<String, String> registerRequest(String email, String nickname, String password) {
        return Map.of(
                "email", email,
                "nickname", nickname,
                "password", password
        );
    }

    private static Map<String, String> loginRequest(String email, String password) {
        return Map.of(
                "email", email,
                "password", password
        );
    }

    private static Map<String, String> updateRequest(String nickname) {
        return Map.of("nickname", nickname);
    }
}
