package com.codeit.monew.domain.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.codeit.monew.domain.user.dto.request.UserRegisterRequest;
import com.codeit.monew.domain.user.dto.response.UserDto;
import com.codeit.monew.domain.user.exception.UserException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class UserServiceTest {

    @Autowired
    UserService userService;

    @Test
    @DisplayName("정상적으로 회원가입 성공")
    void register_success() {
        // given
        String email = "test@email.com";
        String nickname = "testNickname";
        String password = "testPassword";
        UserRegisterRequest request = new UserRegisterRequest(email, nickname, password);

        // when
        UserDto response = userService.register(request);

        // then
        assertThat(response.email()).isEqualTo(email);
        assertThat(response.nickname()).isEqualTo(nickname);
    }

    @Test
    @DisplayName("이메일 중복으로 인한 회원가입 실패")
    void register_fail() {
        // given
        String email = "test@email.com";
        String nickname = "testNickname";
        String password = "testPassword";
        UserRegisterRequest request = new UserRegisterRequest(email, nickname, password);

        // when & then
        userService.register(request);
        assertThatThrownBy(() -> userService.register(request))
                .isInstanceOf(UserException.class);
    }
}