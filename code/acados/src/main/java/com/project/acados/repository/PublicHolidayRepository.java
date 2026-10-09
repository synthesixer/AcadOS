package com.project.acados.repository;

import com.project.acados.domain.entity.PublicHoliday;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for PublicHoliday entity.
 * Reference: class diagram.puml, Implement_Plan-AcadOS.md §7
 */
@Repository
public interface PublicHolidayRepository extends JpaRepository<PublicHoliday, Long> {

    Optional<PublicHoliday> findByDateAndName(LocalDate date, String name);

    boolean existsByDateAndName(LocalDate date, String name);

    List<PublicHoliday> findByDateBetween(LocalDate startDate, LocalDate endDate);
}

