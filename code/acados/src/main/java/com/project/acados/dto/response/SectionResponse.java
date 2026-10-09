package com.project.acados.dto.response;

import com.project.acados.domain.enums.SectionStatus;

/**
 * Section data returned by the section API.
 */
public record SectionResponse(
        Long id,
        Integer sectionNumber,
        Integer capacity,
        SectionStatus status,
        Long courseId,
        String courseCode,
        String courseTitle
) {
}
