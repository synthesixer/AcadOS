package com.project.acados.notification.strategy;

import com.project.acados.domain.entity.Notification;
import com.project.acados.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * In-app channel: stores the notification in the notifications table.
 */
@Component
@RequiredArgsConstructor
public class InAppNotificationStrategy implements NotificationStrategy {

    private final NotificationRepository notificationRepository;

    @Override
    public boolean supports(NotificationChannel channel) {
        return channel == NotificationChannel.IN_APP;
    }

    @Override
    public void send(NotificationMessage message) {
        Notification notification = Notification.builder()
                .user(message.recipient())
                .type(message.type())
                .title(message.title())
                .message(message.message())
                .build();
        notificationRepository.save(notification);
    }
}
