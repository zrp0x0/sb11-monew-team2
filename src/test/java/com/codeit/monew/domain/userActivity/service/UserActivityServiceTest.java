package com.codeit.monew.domain.userActivity.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.codeit.monew.domain.user.entity.User;
import com.codeit.monew.domain.user.exception.UserErrorCode;
import com.codeit.monew.domain.user.exception.UserException;
import com.codeit.monew.domain.user.repository.UserRepository;
import com.codeit.monew.domain.userActivity.dto.UserActivityDto;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserActivityServiceTest {

    @Mock
    UserRepository userRepository;

    @Mock
    UserActivityReader userActivityReader;

    @InjectMocks
    UserActivityService userActivityService;

    @Test
    @DisplayName("본인은 자신의 활동 내역을 조회할 수 있음")
    void get_success() {
        // given
        UUID userId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        User user = User.create(
                "user@example.com",
                "nickname",
                "$2y$04$CnmQ.L0MoRdQxDev/JnKaOKKDqae5Ja40NMIgep0h7xRbX6jhRzZm"
        );
        UserActivityDto activity = emptyActivity(userId);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userActivityReader.read(userId)).thenReturn(activity);

        // when
        UserActivityDto response = userActivityService.get(userId, userId.toString());

        // then
        assertThat(response).isSameAs(activity);
        verify(userRepository).findById(userId);
        verify(userActivityReader).read(userId);
    }

    @Test
    @DisplayName("요청자 헤더가 없으면 활동 내역 조회에 실패")
    void get_fail_whenRequestUserHeaderMissing() {
        // given
        UUID userId = UUID.fromString("11111111-1111-1111-1111-111111111111");

        // when & then
        assertThatThrownBy(() -> userActivityService.get(userId, null))
                .isInstanceOf(UserException.class)
                .extracting("errorCode")
                .isEqualTo(UserErrorCode.REQUEST_USER_ID_REQUIRED);

        verifyNoInteractions(userRepository, userActivityReader);
    }

    @Test
    @DisplayName("요청자 헤더가 UUID 형식이 아니면 활동 내역 조회에 실패")
    void get_fail_whenRequestUserHeaderInvalid() {
        // given
        UUID userId = UUID.fromString("11111111-1111-1111-1111-111111111111");

        // when & then
        assertThatThrownBy(() -> userActivityService.get(userId, "invalid-user-id"))
                .isInstanceOf(UserException.class)
                .extracting("errorCode")
                .isEqualTo(UserErrorCode.REQUEST_USER_ID_REQUIRED);

        verifyNoInteractions(userRepository, userActivityReader);
    }

    @Test
    @DisplayName("요청자 ID와 조회 대상 userId가 다르면 활동 내역 조회에 실패")
    void get_fail_whenRequestUserMismatched() {
        // given
        UUID userId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID requestUserId = UUID.fromString("22222222-2222-2222-2222-222222222222");

        // when & then
        assertThatThrownBy(() -> userActivityService.get(userId, requestUserId.toString()))
                .isInstanceOf(UserException.class)
                .extracting("errorCode")
                .isEqualTo(UserErrorCode.USER_ACCESS_DENIED);

        verifyNoInteractions(userRepository, userActivityReader);
    }

    @Test
    @DisplayName("존재하지 않는 사용자의 활동 내역은 조회할 수 없음")
    void get_fail_whenUserNotFound() {
        // given
        UUID userId = UUID.fromString("11111111-1111-1111-1111-111111111111");

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> userActivityService.get(userId, userId.toString()))
                .isInstanceOf(UserException.class)
                .extracting("errorCode")
                .isEqualTo(UserErrorCode.USER_NOT_FOUND);

        verify(userActivityReader, never()).read(any(UUID.class));
    }

    @Test
    @DisplayName("논리삭제된 사용자는 조회되지 않는 사용자와 동일하게 활동 내역 조회에 실패")
    void get_fail_whenSoftDeletedUserExcluded() {
        // given
        UUID userId = UUID.fromString("11111111-1111-1111-1111-111111111111");

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> userActivityService.get(userId, userId.toString()))
                .isInstanceOf(UserException.class)
                .extracting("errorCode")
                .isEqualTo(UserErrorCode.USER_NOT_FOUND);

        verify(userActivityReader, never()).read(any(UUID.class));
    }

    private UserActivityDto emptyActivity(UUID userId) {
        return new UserActivityDto(
                userId,
                List.of(),
                List.of(),
                List.of(),
                List.of()
        );
    }
}
