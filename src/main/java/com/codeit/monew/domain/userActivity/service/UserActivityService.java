package com.codeit.monew.domain.userActivity.service;

import com.codeit.monew.domain.user.exception.UserErrorCode;
import com.codeit.monew.domain.user.exception.UserException;
import com.codeit.monew.domain.user.repository.UserRepository;
import com.codeit.monew.domain.userActivity.dto.UserActivityDto;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserActivityService {

    private final UserRepository userRepository;
    private final UserActivityReader userActivityReader;

    @Transactional(readOnly = true)
    public UserActivityDto get(UUID userId) {
        userRepository.findById(userId)
            .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND));

        return userActivityReader.read(userId);
    }

}
