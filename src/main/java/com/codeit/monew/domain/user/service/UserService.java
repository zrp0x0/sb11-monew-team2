package com.codeit.monew.domain.user.service;

import com.codeit.monew.domain.user.dto.request.UserLoginRequest;
import com.codeit.monew.domain.user.dto.request.UserRegisterRequest;
import com.codeit.monew.domain.user.dto.request.UserUpdateRequest;
import com.codeit.monew.domain.user.dto.response.UserDto;
import com.codeit.monew.domain.user.entity.User;
import com.codeit.monew.domain.user.exception.UserErrorCode;
import com.codeit.monew.domain.user.exception.UserException;
import com.codeit.monew.domain.user.repository.UserRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * 회원가입
     */
    @Transactional
    public UserDto register(UserRegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new UserException(UserErrorCode.EMAIL_DUPLICATION);
        }

        String encodedPassword = passwordEncoder.encode(request.password());
        User newUser = User.create(request.email(), request.nickname(), encodedPassword);

        try {
            User savedUser = userRepository.saveAndFlush(newUser);
            log.info("회원가입 성공. UserId: {}", savedUser.getId());
            return UserDto.from(savedUser);
        } catch (DataIntegrityViolationException e) {
            throw new UserException(UserErrorCode.EMAIL_DUPLICATION);
        }
    }

    @Transactional(readOnly = true)
    public UserDto login(UserLoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new UserException(UserErrorCode.INVALID_CREDENTIALS));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new UserException(UserErrorCode.INVALID_CREDENTIALS);
        }

        return UserDto.from(user);
    }

    @Transactional
    public UserDto update(UUID userId, String requestUserIdHeader, UserUpdateRequest request) {
        UUID requestUserId = parseRequestUserId(requestUserIdHeader);
        if (!userId.equals(requestUserId)) {
            throw new UserException(UserErrorCode.USER_ACCESS_DENIED);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND));
        user.updateNickname(request.nickname());

        return UserDto.from(user);
    }

    private UUID parseRequestUserId(String requestUserIdHeader) {
        if (!StringUtils.hasText(requestUserIdHeader)) {
            throw new UserException(UserErrorCode.REQUEST_USER_ID_REQUIRED);
        }

        try {
            return UUID.fromString(requestUserIdHeader);
        } catch (IllegalArgumentException e) {
            throw new UserException(UserErrorCode.REQUEST_USER_ID_REQUIRED);
        }
    }
}
