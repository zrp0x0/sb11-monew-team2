package com.codeit.monew.domain.user.service;

import com.codeit.monew.domain.user.dto.request.UserRegisterRequest;
import com.codeit.monew.domain.user.dto.response.UserDto;
import com.codeit.monew.domain.user.entity.User;
import com.codeit.monew.domain.user.exception.UserErrorCode;
import com.codeit.monew.domain.user.exception.UserException;
import com.codeit.monew.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        // 이메일 중복 확인
        // TODO: 현재 논리 삭제 후 같은 이메일로 재가입을 시도하면 완전히 새로운 계정이 생김 => 추후 논의 필요 (email = unique의 필요성 등)
        if (userRepository.existsByEmail(request.email())) {
            throw new UserException(UserErrorCode.EMAIL_DUPLICATION);
        }

        // Bcrypt로 비밀번호 암호화
        String encodedPassword = passwordEncoder.encode(request.password());

        // 유저 생성
        User newUser = new User(request.email(), request.nickname(), encodedPassword);
//        User newUser = User.createUser(request.email(), request.nickname(), encodedPassword);
        User savedUser = userRepository.save(newUser);

        log.info("회원가입 요청 성공. UserId: {}", savedUser.getId());
        return UserDto.from(savedUser);
    }
}
