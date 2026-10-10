package com.project.acados.dto.response;

import com.project.acados.domain.enums.SwapStatus;

import java.time.LocalDateTime;

public record SwapResponse(
        Long id,
        Long requestingTeacherId,
        Long requestingScheduleId,
        Long targetTeacherId,
        Long targetScheduleId,
        SwapStatus status,
        LocalDateTime createdAt,
        LocalDateTime respondedAt,
        LocalDateTime reviewedAt,
        String requestingTeacherName,
        String targetTeacherName,
        String requestingScheduleDetails,
        String targetScheduleDetails
) {
    public SwapResponse(
            Long id,
            Long requestingTeacherId,
            Long requestingScheduleId,
            Long targetTeacherId,
            Long targetScheduleId,
            SwapStatus status,
            LocalDateTime createdAt,
            LocalDateTime respondedAt,
            LocalDateTime reviewedAt
    ) {
        this(id, requestingTeacherId, requestingScheduleId, targetTeacherId, targetScheduleId, status, createdAt, respondedAt, reviewedAt, null, null, null, null);
    }
}
