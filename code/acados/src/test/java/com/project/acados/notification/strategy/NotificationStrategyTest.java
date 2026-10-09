package com.project.acados.notification.strategy;

import com.project.acados.domain.entity.Notification;
import com.project.acados.domain.entity.User;
import com.project.acados.domain.enums.NotificationType;
import com.project.acados.domain.enums.UserRole;
import com.project.acados.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotificationStrategyTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private JavaMailSender mailSender;

    private NotificationMessage message;

    @BeforeEach
    void setUp() {
        User recipient = User.builder()
                .id(1L)
                .universityId("S001")
                .email("s001@acados.local")
                .passwordHash("hashed")
                .role(UserRole.STUDENT)
                .build();
        message = new NotificationMessage(recipient, NotificationType.REGISTRATION_SUCCESS,
                "Registration successful", "You are registered in CP353002 section 1.");
    }

    @Test
    void inApp_supportsOnlyInAppChannel() {
        InAppNotificationStrategy strategy = new InAppNotificationStrategy(notificationRepository);

        assertThat(strategy.supports(NotificationChannel.IN_APP)).isTrue();
        assertThat(strategy.supports(NotificationChannel.EMAIL)).isFalse();
    }

    @Test
    void inApp_send_savesUnreadNotificationForRecipient() {
        InAppNotificationStrategy strategy = new InAppNotificationStrategy(notificationRepository);

        strategy.send(message);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        Notification saved = captor.getValue();
        assertThat(saved.getUser()).isSameAs(message.recipient());
        assertThat(saved.getType()).isEqualTo(NotificationType.REGISTRATION_SUCCESS);
        assertThat(saved.getTitle()).isEqualTo("Registration successful");
        assertThat(saved.getMessage()).isEqualTo("You are registered in CP353002 section 1.");
        assertThat(saved.getIsRead()).isFalse();
    }

    @Test
    void email_supportsOnlyEmailChannel() {
        EmailNotificationStrategy strategy = new EmailNotificationStrategy(mailSender, "no-reply@acados.local");

        assertThat(strategy.supports(NotificationChannel.EMAIL)).isTrue();
        assertThat(strategy.supports(NotificationChannel.IN_APP)).isFalse();
    }

    @Test
    void email_send_sendsPlainTextMailToRecipientAddress() {
        EmailNotificationStrategy strategy = new EmailNotificationStrategy(mailSender, "no-reply@acados.local");

        strategy.send(message);

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        SimpleMailMessage mail = captor.getValue();
        assertThat(mail.getFrom()).isEqualTo("no-reply@acados.local");
        assertThat(mail.getTo()).containsExactly("s001@acados.local");
        assertThat(mail.getSubject()).isEqualTo("Registration successful");
        assertThat(mail.getText()).isEqualTo("You are registered in CP353002 section 1.");
    }

    @Test
    void email_send_whenMailServerFails_doesNotThrow() {
        EmailNotificationStrategy strategy = new EmailNotificationStrategy(mailSender, "no-reply@acados.local");
        doThrow(new MailSendException("SMTP unavailable")).when(mailSender).send(any(SimpleMailMessage.class));

        assertThatCode(() -> strategy.send(message)).doesNotThrowAnyException();
    }
}
