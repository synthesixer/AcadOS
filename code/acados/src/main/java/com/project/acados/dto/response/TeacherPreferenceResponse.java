package com.project.acados.dto.response;

public record TeacherPreferenceResponse(
        Long id,
        Long courseId,
        String courseCode,
        String courseTitle,
        Integer priority
) {}

