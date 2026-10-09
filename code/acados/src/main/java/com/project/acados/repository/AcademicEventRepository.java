package com.project.acados.repository;

import com.project.acados.domain.entity.AcademicEvent;
import com.project.acados.domain.enums.AcademicEventType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Spring Data JPA repository for AcademicEvent entity.
 * Used for checking academic calendar periods like registration windows (BR-09).
 * Reference: class diagram.puml, Implement_Plan-AcadOS.md §7
 */
@Repository
public interface AcademicEventRepository extends JpaRepository<AcademicEvent, Long> {

    List<AcademicEvent> findByEventType(AcademicEventType eventType);

    @Query("SELECT e FROM AcademicEvent e WHERE e.eventType = :eventType AND :date BETWEEN e.startDate AND e.endDate")
    List<AcademicEvent> findActiveEventsByTypeAndDate(@Param("eventType") AcademicEventType eventType, @Param("date") LocalDate date);
}

