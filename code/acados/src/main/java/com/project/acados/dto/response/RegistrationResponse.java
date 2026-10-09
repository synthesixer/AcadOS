package com.project.acados.dto.response;

import java.time.LocalDateTime;

/**
 * Registration data returned by the registration API.
 */
public record RegistrationResponse(
        Long id,
        LocalDateTime registeredAt,
        Long studentId,
        String studentName,
        Long sectionId,
        Integer sectionNumber,
        Long courseId,
        String courseCode,
        String courseTitle
) {
}
