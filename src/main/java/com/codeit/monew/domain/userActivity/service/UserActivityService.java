package com.codeit.monew.domain.userActivity.service;

import com.codeit.monew.domain.user.exception.UserErrorCode;
import com.codeit.monew.domain.user.exception.UserException;
import com.codeit.monew.domain.user.repository.UserRepository;
import com.codeit.monew.domain.userActivity.dto.UserActivityDto;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class UserActivityService {

    private final UserRepository userRepository;
    private final UserActivityReader userActivityReader;

    @Transactional(readOnly = true)
    public UserActivityDto get(UUID userId, String requestUserIdHeader) {
        UUID requestUserId = parseRequestUserId(requestUserIdHeader);
        if (!userId.equals(requestUserId)) {
            throw new UserException(UserErrorCode.USER_ACCESS_DENIED);
        }

        if (!userRepository.existsById(userId)) {
            throw new UserException(UserErrorCode.USER_NOT_FOUND);
        }

        return userActivityReader.read(userId);
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
