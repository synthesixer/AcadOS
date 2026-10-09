package com.project.acados.domain.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

/**
 * Records whether a teacher is available during a time slot.
 * Reference: database.md §2.12, class diagram.puml
 */
@Entity
@Table(
    name = "teacher_availabilities",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_teacher_availabilities_teacher_timeslot",
            columnNames = {"teacher_id", "time_slot_id"}
        )
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeacherAvailability {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id", nullable = false)
    private Teacher teacher;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "time_slot_id", nullable = false)
    private TimeSlot timeSlot;

    @Column(name = "is_available", nullable = false)
    private boolean isAvailable;
}
