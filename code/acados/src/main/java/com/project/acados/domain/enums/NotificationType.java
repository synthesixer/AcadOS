package com.project.acados.domain.enums;

/**
 * Notification type stored in notifications.type (VARCHAR(40), no CHECK constraint).
 * Reference: database.md §2.14
 */
public enum NotificationType {
    REGISTRATION_SUCCESS,
    REGISTRATION_WITHDRAWN,
    SCHEDULE_CHANGED,
    SWAP_REQUESTED,
    SWAP_RESPONDED,
    SWAP_APPROVED,
    SWAP_REJECTED,
    SWAP_CANCELLED,
    SECTION_CANCELLED,
    CONFLICT_DETECTED
}
