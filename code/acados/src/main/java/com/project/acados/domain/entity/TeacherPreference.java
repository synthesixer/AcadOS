package com.project.acados.domain.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

/**
 * Teacher preference for a course, ordered by priority.
 * Reference: database.md §2.11, class diagram.puml
 */
@Entity
@Table(
    name = "teacher_preferences",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_teacher_preferences_teacher_course",
            columnNames = {"teacher_id", "course_id"}
        )
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeacherPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id", nullable = false)
    private Teacher teacher;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @NotNull
    @Min(1)
    @Column(name = "priority", nullable = false)
    private Integer priority;
}
