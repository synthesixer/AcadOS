package com.project.acados.dto.response;

public record UserProfileResponse(
        Long id,
        String universityId,
        String email,
        String role,
        String fullName,
        Long teacherId,
        Long studentId
) {}

