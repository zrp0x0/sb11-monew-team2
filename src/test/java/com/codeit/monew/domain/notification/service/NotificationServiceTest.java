package com.codeit.monew.domain.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.codeit.monew.domain.notification.dto.NotificationDto;
import com.codeit.monew.domain.notification.dto.NotificationSearchCondition;
import com.codeit.monew.domain.notification.entity.Notification;
import com.codeit.monew.domain.notification.entity.NotificationResourceType;
import com.codeit.monew.domain.notification.exception.NotificationErrorCode;
import com.codeit.monew.domain.notification.exception.NotificationException;
import com.codeit.monew.domain.notification.listener.NotificationCreateEvent;
import com.codeit.monew.domain.notification.repository.NotificationRepository;
import com.codeit.monew.domain.user.entity.User;
import com.codeit.monew.domain.user.exception.UserErrorCode;
import com.codeit.monew.domain.user.exception.UserException;
import com.codeit.monew.domain.user.repository.UserRepository;
import com.codeit.monew.global.dto.CursorPageResponse;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @InjectMocks
    private NotificationService notificationService;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private UserRepository userRepository;

    @Nested
    @DisplayName("알림 목록 조회 테스트")
    class GetNotificationsTest {

        @Test
        @DisplayName("성공: 조건에 맞는 알림 목록을 CursorPageResponse 형태로 반환한다.")
        void getNotifications_success() {
            // given
            UUID userId = UUID.randomUUID();
            NotificationSearchCondition condition = new NotificationSearchCondition(null, null, 10);

            User mockUser = User.create("test@email.com", "testNickname", "testPassword");
            ReflectionTestUtils.setField(mockUser, "id", userId);

            Notification notification = Notification.create(mockUser, "테스트 알림",
                NotificationResourceType.ARTICLE, UUID.randomUUID());
            ReflectionTestUtils.setField(notification, "id", UUID.randomUUID());
            ReflectionTestUtils.setField(notification, "createdAt", LocalDateTime.now());

            CursorPageResponse<Notification> mockPageResponse = new CursorPageResponse<>(
                List.of(notification), null, null, 10, 1L, false
            );

            given(notificationRepository.searchNotifications(condition, userId))
                .willReturn(mockPageResponse);

            // when
            CursorPageResponse<NotificationDto> result = notificationService.getNotifications(
                condition, userId);

            // then
            assertThat(result.content()).hasSize(1);
            assertThat(result.content().get(0).content()).isEqualTo("테스트 알림");
            verify(notificationRepository).searchNotifications(condition, userId);
        }
    }

    @Nested
    @DisplayName("전체 알림 확인 테스트")
    class ConfirmAllNotificationTest {

        @Test
        @DisplayName("성공: 존재하는 유저일 경우 전체 미확인 알림을 읽음 처리한다.")
        void confirmAllNotification_success() {
            // given
            UUID userId = UUID.randomUUID();
            User mockUser = User.create("test@email.com", "testNickname", "testPassword");
            ReflectionTestUtils.setField(mockUser, "id", userId);

            given(userRepository.findById(userId)).willReturn(Optional.of(mockUser));
            given(notificationRepository.confirmAllByUserId(eq(userId),
                any(LocalDateTime.class))).willReturn(3);

            // when
            notificationService.confirmAllNotification(userId);

            // then
            verify(userRepository).findById(userId);
            verify(notificationRepository).confirmAllByUserId(eq(userId), any(LocalDateTime.class));
        }

        @Test
        @DisplayName("실패: 존재하지 않는 유저일 경우 예외가 발생한다.")
        void confirmAllNotification_fail_userNotFound() {
            // given
            UUID userId = UUID.randomUUID();
            given(userRepository.findById(userId)).willReturn(Optional.empty());

            // when & then
            UserException exception = assertThrows(UserException.class, () ->
                notificationService.confirmAllNotification(userId)
            );

            assertThat(exception.getErrorCode()).isEqualTo(UserErrorCode.USER_NOT_FOUND);
            verify(notificationRepository, never()).confirmAllByUserId(any(), any());
        }
    }

    @Nested
    @DisplayName("알림 단건 확인 테스트")
    class ConfirmNotificationTest {

        @Test
        @DisplayName("성공: 존재하는 알림과 유저일 경우 알림이 읽음 처리된다.")
        void confirmNotification_success() {
            // given
            UUID notificationId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();

            User mockUser = User.create("test@email.com", "testNickname", "testPassword");
            ReflectionTestUtils.setField(mockUser, "id", userId);

            Notification notification = Notification.create(mockUser, "내용",
                NotificationResourceType.COMMENT, UUID.randomUUID());

            given(notificationRepository.findById(notificationId)).willReturn(
                Optional.of(notification));
            given(userRepository.findById(userId)).willReturn(Optional.of(mockUser));

            // when
            notificationService.confirmNotification(notificationId, userId);

            // then
            assertThat(notification.isConfirmed()).isTrue();
            verify(notificationRepository).findById(notificationId);
            verify(userRepository).findById(userId);
        }

        @Test
        @DisplayName("실패: 존재하지 않는 알림일 경우 예외가 발생한다.")
        void confirmNotification_fail_notificationNotFound() {
            // given
            UUID notificationId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();

            given(notificationRepository.findById(notificationId)).willReturn(Optional.empty());

            // when & then
            NotificationException exception = assertThrows(NotificationException.class, () ->
                notificationService.confirmNotification(notificationId, userId)
            );

            assertThat(exception.getErrorCode()).isEqualTo(
                NotificationErrorCode.NOTIFICATION_NOT_FOUND);
            verify(userRepository, never()).findById(any());
        }

        @Test
        @DisplayName("실패: 알림은 존재하지만 요청한 유저를 찾을 수 없을 경우 예외가 발생한다.")
        void confirmNotification_fail_userNotFound() {
            // given
            UUID notificationId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();

            User mockUser = User.create("test@email.com", "testNickname", "testPassword");
            Notification notification = Notification.create(mockUser, "내용",
                NotificationResourceType.COMMENT, UUID.randomUUID());

            given(notificationRepository.findById(notificationId)).willReturn(
                Optional.of(notification));
            given(userRepository.findById(userId)).willReturn(Optional.empty());

            // when & then
            UserException exception = assertThrows(UserException.class, () ->
                notificationService.confirmNotification(notificationId, userId)
            );

            assertThat(exception.getErrorCode()).isEqualTo(UserErrorCode.USER_NOT_FOUND);
            assertThat(notification.isConfirmed()).isFalse(); // 예외가 터졌으므로 상태가 변하지 않아야 함
        }
    }

    @Nested
    @DisplayName("알림 생성 (이벤트 리스너) 테스트")
    class CreateNotificationTest {

        @Test
        @DisplayName("성공: 존재하는 수신자일 경우 새로운 알림이 저장된다.")
        void createNotification_success() {
            // given
            UUID receiverId = UUID.randomUUID();
            User mockUser = User.create("test@email.com", "testNickname", "testPassword");
            ReflectionTestUtils.setField(mockUser, "id", receiverId);
            
            NotificationCreateEvent event = new NotificationCreateEvent(
                receiverId, "새로운 댓글이 달렸습니다.", NotificationResourceType.COMMENT, UUID.randomUUID()
            );

            given(userRepository.findById(receiverId)).willReturn(Optional.of(mockUser));

            // when
            notificationService.createNotification(event);

            // then
            verify(userRepository).findById(receiverId);
            verify(notificationRepository).save(any(Notification.class));
        }

        @Test
        @DisplayName("성공(무시): 수신자가 존재하지 않거나 탈퇴한 유저일 경우, 에러 없이 알림 생성을 무시한다.")
        void createNotification_ignore_receiverNotFound() {
            // given
            UUID receiverId = UUID.randomUUID();
            NotificationCreateEvent event = new NotificationCreateEvent(
                receiverId, "새로운 댓글이 달렸습니다.", NotificationResourceType.COMMENT, UUID.randomUUID()
            );

            given(userRepository.findById(receiverId)).willReturn(Optional.empty());

            // when
            notificationService.createNotification(event);

            // then
            verify(userRepository).findById(receiverId);
            verify(notificationRepository, never()).save(
                any(Notification.class)); // save 로직이 타지 않아야 함
        }
    }
}