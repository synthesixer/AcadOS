package com.project.acados.service.impl;

import com.project.acados.domain.entity.PublicHoliday;
import com.project.acados.pattern.holiday.HolidayProvider;
import com.project.acados.repository.PublicHolidayRepository;
import com.project.acados.service.HolidayService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Implementation of HolidayService.
 * Coordinates HolidayProvider adapter and PublicHolidayRepository.
 * Reference: class diagram.puml, Implement_Plan-AcadOS.md §7, §10.2, §14.5
 */
@Service
@RequiredArgsConstructor
@Transactional
public class HolidayServiceImpl implements HolidayService {

    private static final Logger log = LoggerFactory.getLogger(HolidayServiceImpl.class);

    private final HolidayProvider holidayProvider;
    private final PublicHolidayRepository publicHolidayRepository;

    @Override
    @Transactional(readOnly = true)
    public List<PublicHoliday> getHolidays() {
        return publicHolidayRepository.findAll();
    }

    @Override
    public void syncHolidays() {
        syncHolidays(LocalDate.now().getYear());
    }

    @Override
    public void syncHolidays(int year) {
        log.info("Starting synchronization of public holidays for year {}", year);
        List<PublicHoliday> externalHolidays = holidayProvider.fetchHolidays(year);

        if (externalHolidays.isEmpty()) {
            log.warn("No public holidays fetched from provider for year {}", year);
            return;
        }

        int addedCount = 0;
        for (PublicHoliday holiday : externalHolidays) {
            boolean exists = publicHolidayRepository.existsByDateAndName(holiday.getDate(), holiday.getName());
            if (!exists) {
                publicHolidayRepository.save(holiday);
                addedCount++;
            }
        }

        log.info("Holiday synchronization completed for year {}: added {} new holidays", year, addedCount);
    }
}

