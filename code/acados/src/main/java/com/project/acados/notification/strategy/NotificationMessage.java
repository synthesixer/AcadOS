package com.project.acados.notification.strategy;

import com.project.acados.domain.entity.User;
import com.project.acados.domain.enums.NotificationType;

/**
 * Data of one notification handed to a NotificationStrategy.
 */
public record NotificationMessage(User recipient, NotificationType type, String title, String message) {
}
