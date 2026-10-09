package com.project.acados.service;

import com.project.acados.domain.entity.Notification;
import com.project.acados.domain.entity.User;
import com.project.acados.domain.enums.NotificationType;
import com.project.acados.notification.strategy.NotificationChannel;

import java.util.List;

/**
 * Sends notifications to users and reads a user's notifications.
 * Reference: class diagram.puml (service), Sequence_diagram.md 04-06
 */
public interface NotificationService {

    /**
     * Sends one notification to the recipient through every requested channel.
     *
     * @throws IllegalArgumentException when no channel is given
     */
    void sendNotification(User recipient, NotificationType type, String title, String message,
                          NotificationChannel... channels);

    /**
     * Returns the user's notifications, newest first.
     */
    List<Notification> getNotifications(Long userId);
}
