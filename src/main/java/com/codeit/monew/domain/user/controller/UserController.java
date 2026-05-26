package com.codeit.monew.domain.user.controller;

import com.codeit.monew.domain.user.dto.request.UserLoginRequest;
import com.codeit.monew.domain.user.dto.request.UserRegisterRequest;
import com.codeit.monew.domain.user.dto.response.UserDto;
import com.codeit.monew.domain.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    @Operation(summary = "회원가입", description = "새로운 사용자를 등록합니다.")
    @PostMapping()
    @ResponseStatus(HttpStatus.CREATED)
    public UserDto registerUser(@Valid @RequestBody UserRegisterRequest request) {
        log.info("회원가입 요청 시작");
        return userService.register(request);
    }

    @Operation(summary = "로그인", description = "이메일과 비밀번호로 로그인합니다.")
    @PostMapping("/login")
    public UserDto login(@Valid @RequestBody UserLoginRequest request) {
        log.info("로그인 요청 시작");
        return userService.login(request);
    }
}
