package com.project.acados.dto.response;

import com.project.acados.domain.enums.NotificationType;

import java.time.LocalDateTime;

/**
 * Notification data returned by the notification API.
 */
public record NotificationResponse(
        Long id,
        NotificationType type,
        String title,
        String message,
        Boolean isRead,
        LocalDateTime createdAt
) {
}
