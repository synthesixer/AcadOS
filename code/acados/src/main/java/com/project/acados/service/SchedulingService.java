package com.project.acados.service;

import com.project.acados.domain.entity.Schedule;

import java.util.List;

/**
 * Orchestrator service interface for timetable scheduling.
 * Reference: Implement_Plan-AcadOS.md §7 (service/), §10.2, §12
 */
public interface SchedulingService {

    /**
     * Generates a draft timetable for all active sections.
     * Deletes previous DRAFT schedules and persists newly calculated schedules with status DRAFT.
     */
    void generateSchedule();

    /**
     * Publishes all current DRAFT schedules to PUBLISHED status.
     */
    void publishSchedule();

    /**
     * Discards all current DRAFT schedules.
     */
    void discardDraft();

    /**
     * Retrieves all current DRAFT schedules.
     */
    List<Schedule> getDraftSchedules();

    /**
     * Retrieves all PUBLISHED schedules.
     */
    List<Schedule> getPublishedSchedules();
}

