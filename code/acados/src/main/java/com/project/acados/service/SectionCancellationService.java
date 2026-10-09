package com.project.acados.service;

/**
 * Cancels a section and handles everything that depends on it.
 * Reference: class diagram.puml (service), Implement_Plan-AcadOS.md §14.4, Userflow A08
 */
public interface SectionCancellationService {

    /**
     * Sets the section to CANCELLED (State Pattern), deletes its registrations, releases its
     * periods and notifies the affected students and teachers.
     *
     * @throws com.project.acados.exception.ResourceNotFoundException when the section does not exist
     * @throws com.project.acados.exception.BusinessRuleException when the section is already cancelled
     */
    void cancelSection(Long sectionId);
}
