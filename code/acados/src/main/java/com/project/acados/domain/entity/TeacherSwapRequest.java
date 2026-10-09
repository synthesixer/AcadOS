package com.project.acados.domain.entity;

import com.project.acados.domain.enums.SwapStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entity representing a timetable swap request between two teachers.
 * Stores teacher snapshot audit fields and allows ON DELETE SET NULL for schedule references.
 * Reference: database.md §2.13, class diagram.puml
 */
@Entity
@Table(name = "teacher_swap_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeacherSwapRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requesting_teacher_id", nullable = false)
    private Teacher requestingTeacher;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requesting_schedule_id", nullable = true)
    private Schedule requestingSchedule;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_teacher_id", nullable = false)
    private Teacher targetTeacher;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_schedule_id", nullable = true)
    private Schedule targetSchedule;

    @NotNull
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SwapStatus status = SwapStatus.PENDING;

    @NotNull
    @Builder.Default
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "responded_at")
    private LocalDateTime respondedAt;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    public void accept() {
        this.status = SwapStatus.ACCEPTED;
        this.respondedAt = LocalDateTime.now();
    }

    public void reject() {
        this.status = SwapStatus.REJECTED;
        this.respondedAt = LocalDateTime.now();
    }

    public void approve() {
        this.status = SwapStatus.APPROVED;
        this.reviewedAt = LocalDateTime.now();
    }

    public void cancel() {
        this.status = SwapStatus.CANCELLED;
    }
}

