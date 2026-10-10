package com.project.acados.service;

import com.project.acados.domain.entity.Notification;
import com.project.acados.domain.entity.User;
import com.project.acados.domain.enums.NotificationType;
import com.project.acados.notification.strategy.NotificationChannel;
import com.project.acados.pattern.observer.ScheduleChangeObserver;

import java.util.List;

/**
 * Sends notifications to users and reads a user's notifications.
 * It is also the observer of schedule changes (Observer Pattern): onScheduleChanged sends
 * SCHEDULE_CHANGED to the students and teachers of the changed sections.
 * Reference: class diagram.puml (service, observer), Sequence_diagram.md 04-06
 */
public interface NotificationService extends ScheduleChangeObserver {

    /**
     * Sends one notification to the recipient through every requested channel.
     *
     * @throws IllegalArgumentException when no channel is given
     */
    void sendNotification(User recipient, NotificationType type, String title, String message,
                          NotificationChannel... channels);

    /**
     * Same as sendNotification but in its own transaction, so the notification is kept
     * when the caller's transaction rolls back (used for CONFLICT_DETECTED, Sequence 04).
     */
    void sendNotificationInNewTransaction(User recipient, NotificationType type, String title, String message,
                                          NotificationChannel... channels);

    /**
     * Returns the notifications of the user with this university ID, newest first.
     */
    List<Notification> getNotifications(String universityId);

    /**
     * Marks one of the user's own notifications as read.
     *
     * @throws com.project.acados.exception.ResourceNotFoundException when the notification
     *         does not exist or belongs to another user
     */
    void markAsRead(String universityId, Long notificationId);
}
