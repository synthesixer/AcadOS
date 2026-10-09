package com.project.acados.service;

import com.project.acados.domain.entity.Notification;
import com.project.acados.domain.entity.User;
import com.project.acados.domain.enums.NotificationType;
import com.project.acados.domain.enums.UserRole;
import com.project.acados.notification.strategy.NotificationChannel;
import com.project.acados.notification.strategy.NotificationMessage;
import com.project.acados.notification.strategy.NotificationStrategy;
import com.project.acados.repository.NotificationRepository;
import com.project.acados.service.impl.NotificationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock
    private NotificationStrategy inAppStrategy;

    @Mock
    private NotificationStrategy emailStrategy;

    @Mock
    private NotificationRepository notificationRepository;

    private NotificationServiceImpl notificationService;

    private User recipient;

    @BeforeEach
    void setUp() {
        lenient().when(inAppStrategy.supports(any()))
                .thenAnswer(invocation -> invocation.getArgument(0) == NotificationChannel.IN_APP);
        lenient().when(emailStrategy.supports(any()))
                .thenAnswer(invocation -> invocation.getArgument(0) == NotificationChannel.EMAIL);
        notificationService = new NotificationServiceImpl(List.of(inAppStrategy, emailStrategy), notificationRepository);
        recipient = User.builder()
                .id(1L)
                .universityId("S001")
                .email("s001@acados.local")
                .passwordHash("hashed")
                .role(UserRole.STUDENT)
                .build();
    }

    @Test
    void sendNotification_whenInAppOnly_usesOnlyInAppStrategy() {
        notificationService.sendNotification(recipient, NotificationType.CONFLICT_DETECTED,
                "Schedule conflict", "The section overlaps with a registered section.",
                NotificationChannel.IN_APP);

        ArgumentCaptor<NotificationMessage> captor = ArgumentCaptor.forClass(NotificationMessage.class);
        verify(inAppStrategy).send(captor.capture());
        verify(emailStrategy, never()).send(any());

        NotificationMessage sent = captor.getValue();
        assertThat(sent.recipient()).isSameAs(recipient);
        assertThat(sent.type()).isEqualTo(NotificationType.CONFLICT_DETECTED);
        assertThat(sent.title()).isEqualTo("Schedule conflict");
        assertThat(sent.message()).isEqualTo("The section overlaps with a registered section.");
    }

    @Test
    void sendNotification_whenInAppAndEmail_usesBothStrategies() {
        notificationService.sendNotification(recipient, NotificationType.REGISTRATION_SUCCESS,
                "Registration successful", "You are registered.",
                NotificationChannel.IN_APP, NotificationChannel.EMAIL);

        verify(inAppStrategy).send(any());
        verify(emailStrategy).send(any());
    }

    @Test
    void sendNotification_whenChannelRepeated_sendsOncePerChannel() {
        notificationService.sendNotification(recipient, NotificationType.SCHEDULE_CHANGED,
                "Schedule changed", "The timetable has changed.",
                NotificationChannel.IN_APP, NotificationChannel.IN_APP);

        verify(inAppStrategy, times(1)).send(any());
    }

    @Test
    void sendNotification_whenNoChannel_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> notificationService.sendNotification(recipient,
                NotificationType.SCHEDULE_CHANGED, "Schedule changed", "The timetable has changed."))
                .isInstanceOf(IllegalArgumentException.class);

        verify(inAppStrategy, never()).send(any());
        verify(emailStrategy, never()).send(any());
    }

    @Test
    void getNotifications_returnsUserNotificationsFromRepository() {
        Notification notification = Notification.builder()
                .id(10L)
                .user(recipient)
                .type(NotificationType.SCHEDULE_CHANGED)
                .title("Schedule changed")
                .message("The timetable has changed.")
                .build();
        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(notification));

        List<Notification> result = notificationService.getNotifications(1L);

        assertThat(result).containsExactly(notification);
    }
}
