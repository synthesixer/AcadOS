package com.project.acados.service;

import com.project.acados.domain.entity.PublicHoliday;

import java.util.List;

/**
 * Service interface for Public Holiday management.
 * Reference: class diagram.puml (§4 service), Implement_Plan-AcadOS.md §7, §10.2, §16
 */
public interface HolidayService {

    /**
     * Retrieves all public holidays currently stored in the database.
     *
     * @return list of PublicHoliday entities
     */
    List<PublicHoliday> getHolidays();

    /**
     * Syncs public holidays from external provider into database.
     * Prevents duplicate inserts based on date and name.
     */
    void syncHolidays();

    /**
     * Syncs public holidays for a specific year.
     *
     * @param year calendar year (e.g. 2026)
     */
    void syncHolidays(int year);
}

