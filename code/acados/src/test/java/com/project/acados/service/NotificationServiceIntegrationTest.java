package com.project.acados.service;

import com.project.acados.domain.entity.Notification;
import com.project.acados.domain.entity.User;
import com.project.acados.domain.enums.NotificationType;
import com.project.acados.domain.enums.UserRole;
import com.project.acados.notification.strategy.InAppNotificationStrategy;
import com.project.acados.notification.strategy.NotificationChannel;
import com.project.acados.service.impl.NotificationServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Runs NotificationServiceImpl with the real in-app strategy against the test database.
 */
@DataJpaTest
@ActiveProfiles("test")
@Import({NotificationServiceImpl.class, InAppNotificationStrategy.class})
class NotificationServiceIntegrationTest {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private TestEntityManager entityManager;

    private User persistUser(String universityId) {
        return entityManager.persist(User.builder()
                .universityId(universityId)
                .email(universityId.toLowerCase() + "@acados.local")
                .passwordHash("hashed")
                .role(UserRole.STUDENT)
                .build());
    }

    @Test
    void sendNotification_inApp_isStoredAndReturnedOnlyForRecipient() {
        User recipient = persistUser("S001");
        User otherUser = persistUser("S002");

        notificationService.sendNotification(recipient, NotificationType.REGISTRATION_SUCCESS,
                "Registration successful", "You are registered in CP353002 section 1.",
                NotificationChannel.IN_APP);
        entityManager.flush();
        entityManager.clear();

        List<Notification> notifications = notificationService.getNotifications(recipient.getId());
        assertThat(notifications).hasSize(1);
        Notification stored = notifications.get(0);
        assertThat(stored.getType()).isEqualTo(NotificationType.REGISTRATION_SUCCESS);
        assertThat(stored.getTitle()).isEqualTo("Registration successful");
        assertThat(stored.getIsRead()).isFalse();
        assertThat(stored.getCreatedAt()).isNotNull();
        assertThat(notificationService.getNotifications(otherUser.getId())).isEmpty();
    }
}
