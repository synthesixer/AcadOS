package com.project.acados.dto.response;

/**
 * Teacher option info for section teacher assignment dialog.
 */
public record TeacherAssignmentOptionResponse(
        Long teacherId,
        String fullName,
        String universityId,
        boolean qualified
) {
}

