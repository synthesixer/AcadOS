package com.project.acados.domain.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

/**
 * Associates a teacher with a course they are qualified to teach (BR-06).
 * Reference: database.md section 2.10, class diagram.puml.
 */
@Entity
@Table(
    name = "teacher_qualifications",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_teacher_qualifications_teacher_course",
            columnNames = {"teacher_id", "course_id"}
        )
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeacherQualification {

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
}
