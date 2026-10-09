package com.project.acados.service;

import com.project.acados.dto.response.SwapInboxResponse;

/** Read-side operations required by the documented teacher request-status view (T08). */
public interface TeacherSwapQueryService {
    SwapInboxResponse getRequestsForTeacher(Long teacherId);
}
