package com.project.acados.service;

import com.project.acados.domain.entity.TeacherSwapRequest;

/**
 * Business operations for teacher schedule swap requests.
 */
public interface TeacherSwapService {

    TeacherSwapRequest createSwapRequest(
            Long requestingTeacherId,
            Long requestingScheduleId,
            Long targetTeacherId,
            Long targetScheduleId
    );

    void respondSwap(Long requestId, Long respondingTeacherId, boolean accept);

    void approveSwap(Long requestId);

    void rejectSwap(Long requestId);

    void cancelSwap(Long requestId, Long requestingTeacherId);
}
