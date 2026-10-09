package com.project.acados.pattern.holiday;

import com.project.acados.domain.entity.PublicHoliday;

import java.util.List;

/**
 * Target interface for the Holiday Adapter Pattern.
 * Provides abstraction for retrieving public holidays from external sources.
 * Reference: class diagram.puml (§7 adapter), Implement_Plan-AcadOS.md §7, §10.3, §17.4
 */
public interface HolidayProvider {

    /**
     * Fetches public holidays for the current calendar year.
     *
     * @return list of PublicHoliday entities
     */
    List<PublicHoliday> fetchHolidays();

    /**
     * Fetches public holidays for a specific year.
     *
     * @param year calendar year (e.g. 2026)
     * @return list of PublicHoliday entities
     */
    List<PublicHoliday> fetchHolidays(int year);
}

