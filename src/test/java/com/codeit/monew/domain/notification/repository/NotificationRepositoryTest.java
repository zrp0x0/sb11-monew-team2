package com.codeit.monew.domain.notification.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.codeit.monew.domain.notification.dto.NotificationSearchCondition;
import com.codeit.monew.domain.notification.entity.Notification;
import com.codeit.monew.domain.notification.entity.NotificationResourceType;
import com.codeit.monew.domain.user.entity.User;
import com.codeit.monew.domain.user.repository.UserRepository;
import com.codeit.monew.global.config.JpaAuditingConfig;
import com.codeit.monew.global.config.QuerydslConfig;
import com.codeit.monew.global.dto.CursorPageResponse;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.util.ReflectionTestUtils;

@DataJpaTest
@Import({QuerydslConfig.class, JpaAuditingConfig.class})
//@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE) // H2 의존성 추가 시 사용하지 않아도 됨
class NotificationRepositoryTest {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private UserRepository userRepository;

    private User testUser;
    private final List<Notification> savedNotifications = new ArrayList<>();

    @BeforeEach
    void setUp() {
        // 테스트용 유저 생성
        testUser = User.create("test@test.com", "tester", "encodedPassword");
        userRepository.save(testUser);

        // 테스트용 알림 5개 생성 (시간순 정렬 테스트를 위해 createdAt을 1분 간격으로 명시적 조작)
        LocalDateTime baseTime = LocalDateTime.now().minusDays(1);
        for (int i = 0; i < 5; i++) {
            Notification notification = Notification.create(
                testUser, "알림 " + (i + 1), NotificationResourceType.ARTICLE, UUID.randomUUID()
            );
            ReflectionTestUtils.setField(notification, "createdAt", baseTime.plusMinutes(i));
            savedNotifications.add(notificationRepository.save(notification));
        }
    }

    @Test
    @DisplayName("첫 페이지 조회: cursor가 null일 때 지정한 limit만큼 조회되며, totalElementCount가 계산된다.")
    void searchNotifications_firstPage() {
        // given
        int limit = 2;
        NotificationSearchCondition condition = new NotificationSearchCondition(null, null, limit);

        // when
        CursorPageResponse<Notification> response = notificationRepository.searchNotifications(
            condition, testUser.getId());

        // then
        assertThat(response.content()).hasSize(limit);
        assertThat(response.content().get(0).getContent()).isEqualTo("알림 1"); // ASC 정렬 확인
        assertThat(response.content().get(1).getContent()).isEqualTo("알림 2");

        assertThat(response.hasNext()).isTrue();
        assertThat(response.totalElements()).isEqualTo(5L); // 첫 페이지이므로 전체 개수 계산됨

        assertThat(response.nextCursor()).isNotNull();
        assertThat(response.nextAfter()).isNotNull();
    }

    @Test
    @DisplayName("다음 페이지 조회: cursor와 after 값이 주어지면 그 이후의 데이터를 가져오고, totalElementCount는 null이다.")
    void searchNotifications_nextPage() {
        // given
        // 1. 첫 페이지 결과에서 커서 값을 추출하여 다음 페이지 조건 생성
        CursorPageResponse<Notification> firstPage = notificationRepository.searchNotifications(
            new NotificationSearchCondition(null, null, 2), testUser.getId()
        );

        LocalDateTime nextCursor = LocalDateTime.parse(firstPage.nextCursor());
        LocalDateTime nextAfter = LocalDateTime.parse(firstPage.nextAfter());
        NotificationSearchCondition nextCondition = new NotificationSearchCondition(nextCursor,
            nextAfter, 2);

        // when
        CursorPageResponse<Notification> secondPage = notificationRepository.searchNotifications(
            nextCondition, testUser.getId());

        // then
        assertThat(secondPage.content()).hasSize(2);
        assertThat(secondPage.content().get(0).getContent()).isEqualTo("알림 3"); // 알림 2 다음 데이터인지 확인
        assertThat(secondPage.content().get(1).getContent()).isEqualTo("알림 4");

        assertThat(secondPage.hasNext()).isTrue();
        assertThat(secondPage.totalElements()).isNull(); // 두 번째 페이지이므로 count 쿼리가 생략되어야 함
    }

    @Test
    @DisplayName("마지막 페이지 조회: 남은 데이터 수가 limit 이하일 경우 hasNext는 false를 반환한다.")
    void searchNotifications_lastPage() {
        // given
        // 총 5개 중 앞의 4개를 건너뛰는 커서 조건 생성
        LocalDateTime cursor = savedNotifications.get(3).getCreatedAt(); // 알림 4의 시간
        NotificationSearchCondition condition = new NotificationSearchCondition(cursor, cursor, 2);

        // when
        CursorPageResponse<Notification> response = notificationRepository.searchNotifications(
            condition, testUser.getId());

        // then
        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0).getContent()).isEqualTo("알림 5");

        assertThat(response.hasNext()).isFalse();
        assertThat(response.nextCursor()).isNull();
        assertThat(response.nextAfter()).isNull();
    }

    @Test
    @DisplayName("필터링: 읽음 처리(confirmed = true)된 알림은 조회되지 않는다.")
    void searchNotifications_excludeConfirmed() {
        // given
        // 첫 번째와 두 번째 알림을 읽음 처리
        savedNotifications.get(0).confirm();
        savedNotifications.get(1).confirm();
        notificationRepository.flush(); // 영속성 컨텍스트 반영

        NotificationSearchCondition condition = new NotificationSearchCondition(null, null, 10);

        // when
        CursorPageResponse<Notification> response = notificationRepository.searchNotifications(
            condition, testUser.getId());

        // then
        assertThat(response.content()).hasSize(3); // 총 5개 중 2개를 읽었으므로 3개만 남음
        assertThat(response.content()).extracting("confirmed").containsOnly(false);
        assertThat(response.totalElements()).isEqualTo(3L);
    }

    @Test
    @DisplayName("필터링: 다른 유저의 알림은 조회되지 않는다.")
    void searchNotifications_excludeOtherUser() {
        // given
        User otherUser = User.create("other@test.com", "other", "password");
        userRepository.save(otherUser);

        Notification otherUserNotification = Notification.create(
            otherUser, "다른 유저의 알림", NotificationResourceType.COMMENT, UUID.randomUUID()
        );
        notificationRepository.save(otherUserNotification);

        NotificationSearchCondition condition = new NotificationSearchCondition(null, null, 10);

        // when
        // testUser의 ID로 조회
        CursorPageResponse<Notification> response = notificationRepository.searchNotifications(
            condition, testUser.getId());

        // then
        // otherUser의 알림은 섞여 들어오지 않아야 함
        assertThat(response.content()).hasSize(5);
        assertThat(response.content()).extracting("content")
            .doesNotContain("다른 유저의 알림");
    }
}