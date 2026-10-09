package com.project.acados.domain.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.DayOfWeek;
import java.time.LocalTime;

/**
 * TimeSlot entity representing weekly time windows.
 * Reference: database.md §2.7, class diagram.puml
 */
@Entity
@Table(
    name = "time_slots",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_time_slots_day_time", columnNames = {"day_of_week", "start_time", "end_time"})
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TimeSlot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", nullable = false, length = 10)
    private DayOfWeek dayOfWeek;

    @NotNull
    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @NotNull
    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    /**
     * Checks if this TimeSlot overlaps with another TimeSlot on the same day (BR-03, C-01).
     */
    public boolean overlapsWith(TimeSlot other) {
        if (other == null || this.dayOfWeek != other.dayOfWeek) {
            return false;
        }
        return this.startTime.isBefore(other.endTime) && other.startTime.isBefore(this.endTime);
    }
}

