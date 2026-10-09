package com.project.acados.service.impl;

import com.project.acados.domain.entity.Notification;
import com.project.acados.domain.entity.User;
import com.project.acados.domain.enums.NotificationType;
import com.project.acados.notification.strategy.NotificationChannel;
import com.project.acados.notification.strategy.NotificationMessage;
import com.project.acados.notification.strategy.NotificationStrategy;
import com.project.acados.repository.NotificationRepository;
import com.project.acados.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Default NotificationService. Spring injects every NotificationStrategy bean,
 * and each one is asked whether it supports the requested channel.
 */
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final List<NotificationStrategy> strategies;
    private final NotificationRepository notificationRepository;

    @Override
    @Transactional
    public void sendNotification(User recipient, NotificationType type, String title, String message,
                                 NotificationChannel... channels) {
        if (channels == null || channels.length == 0) {
            throw new IllegalArgumentException("At least one notification channel is required");
        }
        Set<NotificationChannel> requestedChannels = EnumSet.copyOf(Arrays.asList(channels));
        NotificationMessage notificationMessage = new NotificationMessage(recipient, type, title, message);

        for (NotificationChannel channel : requestedChannels) {
            for (NotificationStrategy strategy : strategies) {
                if (strategy.supports(channel)) {
                    strategy.send(notificationMessage);
                }
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Notification> getNotifications(Long userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }
}
