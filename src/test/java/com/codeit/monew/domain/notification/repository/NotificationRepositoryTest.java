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
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

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

    @Autowired
    private EntityManager em; // 👈 추가해 주세요

    @BeforeEach
    void setUp() {
        // 테스트용 유저 생성
        testUser = User.create("test@test.com", "tester", "encodedPassword");
        userRepository.save(testUser);

        // 시간 정밀도 버그 방지를 위해 초(Second) 단위로 자름
        LocalDateTime baseTime = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS).minusDays(1);

        // 1. 먼저 DB에 저장을 합니다 (이때 Auditing에 의해 현재 시간으로 다 똑같이 저장됨)
        for (int i = 0; i < 5; i++) {
            Notification notification = Notification.create(
                testUser, "알림 " + (i + 1), NotificationResourceType.ARTICLE, UUID.randomUUID()
            );
            notificationRepository.save(notification);
        }
        notificationRepository.flush();

        // 2. 쿼리를 날려 시간을 1분 간격으로 완벽하게 강제 조작(Auditing 우회)
        List<Notification> all = notificationRepository.findAll();
        for (int i = 0; i < all.size(); i++) {
            em.createQuery("UPDATE Notification n SET n.createdAt = :time WHERE n.id = :id")
                .setParameter("time", baseTime.plusMinutes(i))
                .setParameter("id", all.get(i).getId())
                .executeUpdate();
        }
        em.clear(); // 1차 캐시를 비워서 이후 조회 시 DB에서 새 시간을 가져오게 함

        // 3. 검증용 리스트를 완벽한 상태로 다시 채움
        savedNotifications.clear();
        savedNotifications.addAll(notificationRepository.findAll());
        savedNotifications.sort(Comparator.comparing(Notification::getCreatedAt)); // 확실한 오름차순 정렬
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
        // 1. 처음 4개를 먼저 조회하여 정확한 커서 값을 얻어냄
        CursorPageResponse<Notification> previousPage = notificationRepository.searchNotifications(
            new NotificationSearchCondition(null, null, 4), testUser.getId()
        );

        // 2. 조회된 결과에서 커서 값을 파싱합니다 (DB에서 읽어온 정확한 문자열 값).
        LocalDateTime nextCursor = LocalDateTime.parse(previousPage.nextCursor());
        LocalDateTime nextAfter = LocalDateTime.parse(previousPage.nextAfter());

        // 3. limit을 2로 주더라도 남은 데이터가 1개뿐이므로 1개만 조회
        NotificationSearchCondition condition = new NotificationSearchCondition(nextCursor,
            nextAfter, 2);

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