package com.project.acados.domain.enums;

/**
 * Status of a teacher timetable swap request.
 * Reference: database.md §2.13, class diagram.puml
 */
public enum SwapStatus {
    PENDING,
    ACCEPTED,
    REJECTED,
    APPROVED,
    CANCELLED
}

