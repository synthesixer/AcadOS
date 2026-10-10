package com.project.acados.service;

import com.project.acados.dto.response.SwapInboxResponse;
import com.project.acados.dto.response.SwapResponse;

import java.util.List;

/** Read-side operations required by teacher request-status view (T08) and admin swap review (A08). */
public interface TeacherSwapQueryService {
    SwapInboxResponse getRequestsForTeacher(Long teacherId);

    List<SwapResponse> getAllRequests();
}
