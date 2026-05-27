package com.codeit.monew.domain.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.codeit.monew.domain.user.dto.request.UserLoginRequest;
import com.codeit.monew.domain.user.dto.request.UserRegisterRequest;
import com.codeit.monew.domain.user.dto.response.UserDto;
import com.codeit.monew.domain.user.entity.User;
import com.codeit.monew.domain.user.exception.UserErrorCode;
import com.codeit.monew.domain.user.exception.UserException;
import com.codeit.monew.domain.user.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    UserRepository userRepository;

    @Mock
    PasswordEncoder passwordEncoder;

    @InjectMocks
    UserService userService;

    @Test
    @DisplayName("유효한 정보로 회원가입하면 비밀번호를 해시 저장하고 UserDto 반환")
    void register_success() {
        // given
        String email = "test@email.com";
        String nickname = "testNickname";
        String password = "testPassword";
        String encodedPassword = "$2y$04$CnmQ.L0MoRdQxDev/JnKaOKKDqae5Ja40NMIgep0h7xRbX6jhRzZm";
        UserRegisterRequest request = new UserRegisterRequest(email, nickname, password);

        when(userRepository.existsByEmail(email)).thenReturn(false);
        when(passwordEncoder.encode(password)).thenReturn(encodedPassword);
        when(userRepository.saveAndFlush(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // when
        UserDto response = userService.register(request);

        // then
        assertThat(response.email()).isEqualTo(email);
        assertThat(response.nickname()).isEqualTo(nickname);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).saveAndFlush(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertThat(savedUser.getEmail()).isEqualTo(email);
        assertThat(savedUser.getNickname()).isEqualTo(nickname);
        assertThat(savedUser.getPasswordHash()).isEqualTo(encodedPassword);
        assertThat(savedUser.getPasswordHash()).isNotEqualTo(password);
    }

    @Test
    @DisplayName("이미 존재하는 이메일은 회원가입에 실패")
    void register_fail_whenEmailAlreadyExists() {
        // given
        String email = "test@email.com";
        String nickname = "testNickname";
        String password = "testPassword";
        UserRegisterRequest request = new UserRegisterRequest(email, nickname, password);

        when(userRepository.existsByEmail(email)).thenReturn(true);

        // when & then
        assertThatThrownBy(() -> userService.register(request))
                .isInstanceOf(UserException.class)
                .extracting("errorCode")
                .isEqualTo(UserErrorCode.EMAIL_DUPLICATION);

        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).saveAndFlush(any(User.class));
    }

    @Test
    @DisplayName("DB unique 제약 위반 시 이메일 중복 예외 처리")
    void register_fail_whenEmailUniqueConstraintViolated() {
        // given
        String email = "test@email.com";
        String nickname = "testNickname";
        String password = "testPassword";
        String encodedPassword = "$2y$04$CnmQ.L0MoRdQxDev/JnKaOKKDqae5Ja40NMIgep0h7xRbX6jhRzZm";
        UserRegisterRequest request = new UserRegisterRequest(email, nickname, password);

        when(userRepository.existsByEmail(email)).thenReturn(false);
        when(passwordEncoder.encode(password)).thenReturn(encodedPassword);
        when(userRepository.saveAndFlush(any(User.class)))
                .thenThrow(new DataIntegrityViolationException("uk_users_email"));

        // when & then
        assertThatThrownBy(() -> userService.register(request))
                .isInstanceOf(UserException.class)
                .extracting("errorCode")
                .isEqualTo(UserErrorCode.EMAIL_DUPLICATION);
    }

    @Test
    @DisplayName("올바른 이메일과 비밀번호로 로그인하면 UserDto를 반환")
    void login_success() {
        // given
        String email = "test@email.com";
        String password = "testPassword";
        String encodedPassword = "$2y$04$CnmQ.L0MoRdQxDev/JnKaOKKDqae5Ja40NMIgep0h7xRbX6jhRzZm";
        UserLoginRequest request = new UserLoginRequest(email, password);
        User user = User.create(email, "testNickname", encodedPassword);

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(password, encodedPassword)).thenReturn(true);

        // when
        UserDto response = userService.login(request);

        // then
        assertThat(response.email()).isEqualTo(email);
        assertThat(response.nickname()).isEqualTo("testNickname");
        verify(passwordEncoder).matches(password, encodedPassword);
    }

    @Test
    @DisplayName("존재하지 않는 이메일이면 로그인에 실패")
    void login_fail_whenEmailNotFound() {
        // given
        String email = "unknown@email.com";
        UserLoginRequest request = new UserLoginRequest(email, "testPassword");

        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> userService.login(request))
                .isInstanceOf(UserException.class)
                .extracting("errorCode")
                .isEqualTo(UserErrorCode.INVALID_CREDENTIALS);

        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }

    @Test
    @DisplayName("비밀번호가 일치하지 않으면 로그인에 실패")
    void login_fail_whenPasswordMismatched() {
        // given
        String email = "test@email.com";
        String password = "wrongPassword";
        String encodedPassword = "$2y$04$CnmQ.L0MoRdQxDev/JnKaOKKDqae5Ja40NMIgep0h7xRbX6jhRzZm";
        UserLoginRequest request = new UserLoginRequest(email, password);
        User user = User.create(email, "testNickname", encodedPassword);

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(password, encodedPassword)).thenReturn(false);

        // when & then
        assertThatThrownBy(() -> userService.login(request))
                .isInstanceOf(UserException.class)
                .extracting("errorCode")
                .isEqualTo(UserErrorCode.INVALID_CREDENTIALS);
    }

    @Test
    @DisplayName("논리삭제 사용자는 조회되지 않는 사용자와 동일하게 로그인에 실패")
    void login_fail_whenSoftDeletedUserExcluded() {
        // given
        String email = "deleted@email.com";
        UserLoginRequest request = new UserLoginRequest(email, "testPassword");

        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> userService.login(request))
                .isInstanceOf(UserException.class)
                .extracting("errorCode")
                .isEqualTo(UserErrorCode.INVALID_CREDENTIALS);

        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }
}
