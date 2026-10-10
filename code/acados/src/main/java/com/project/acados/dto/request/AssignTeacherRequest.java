package com.project.acados.dto.request;

import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for assigning a teacher to a section (Sub-feature A13).
 * Reference: Userflow A13, BR-06, Implement_Plan-AcadOS.md §16
 */
public record AssignTeacherRequest(
        @NotNull(message = "Teacher ID is required")
        Long teacherId
) {
}

