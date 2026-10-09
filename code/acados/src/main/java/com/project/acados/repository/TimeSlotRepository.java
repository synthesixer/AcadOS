package com.project.acados.repository;

import com.project.acados.domain.entity.TimeSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for TimeSlot entity.
 * Reference: class diagram.puml, Implement_Plan-AcadOS.md §7
 */
@Repository
public interface TimeSlotRepository extends JpaRepository<TimeSlot, Long> {

    Optional<TimeSlot> findByDayOfWeekAndStartTimeAndEndTime(DayOfWeek dayOfWeek, LocalTime startTime, LocalTime endTime);

    List<TimeSlot> findByDayOfWeek(DayOfWeek dayOfWeek);

    List<TimeSlot> findByDayOfWeekOrderByStartTimeAsc(DayOfWeek dayOfWeek);
}

