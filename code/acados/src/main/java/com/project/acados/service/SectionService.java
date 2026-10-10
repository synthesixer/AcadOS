package com.project.acados.service;

import com.project.acados.domain.entity.Section;
import com.project.acados.dto.request.SectionRequest;

import java.util.List;

/**
 * Section management: read, create and update capacity. Sections are never deleted;
 * SectionCancellationService cancels them instead.
 * Reference: class diagram.puml (service), Userflow A12-1 to A12-3
 */
public interface SectionService {

    /**
     * Returns the sections of one course, or of every course when courseId is null.
     */
    List<Section> getSections(Long courseId);

    Section getSection(Long id);

    Section createSection(SectionRequest request);

    /**
     * Changes the capacity of an ACTIVE section (BR-05: not below the registered count).
     */
    Section updateSection(Long id, SectionRequest request);

    /**
     * Assigns a teacher to all schedules of the section (Sub-feature A13).
     * Validates qualification (BR-06), availability (BR-07), schedule conflict (BR-01),
     * and ensures schedules are PUBLISHED.
     */
    Section assignTeacher(Long id, Long teacherId);

    /**
     * Returns all teachers indicating whether each is qualified for the section's course.
     */
    List<com.project.acados.dto.response.TeacherAssignmentOptionResponse> getTeacherOptions(Long id);
}
