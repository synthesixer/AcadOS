package com.project.acados.service;

import com.project.acados.domain.entity.Registration;

/**
 * Student course registration and withdrawal.
 * Reference: class diagram.puml (service), Sequence_diagram.md 04, Implement_Plan-AcadOS.md §14.1-14.2
 */
public interface RegistrationService {

    /**
     * Registers the student in a section after checking BR-09, section status, BR-04, BR-05 and BR-03.
     *
     * @param universityId university ID of the signed-in student
     * @throws com.project.acados.exception.BusinessRuleException when a rule is broken
     * @throws com.project.acados.exception.ResourceNotFoundException when the student or section is not found
     */
    Registration register(String universityId, Long sectionId);

    /**
     * Withdraws one of the student's own registrations (hard delete) inside the registration period.
     *
     * @throws com.project.acados.exception.BusinessRuleException when outside the registration period (BR-09)
     * @throws com.project.acados.exception.ResourceNotFoundException when the registration is not the student's
     */
    void withdraw(String universityId, Long registrationId);

    /**
     * BR-04: true when the student is already registered in a section of this course.
     */
    boolean checkDuplicate(Long studentId, Long courseId);

    /**
     * BR-03: true when a PUBLISHED period of the section overlaps a PUBLISHED period
     * of a section the student is already registered in.
     */
    boolean checkConflict(Long studentId, Long sectionId);
}
