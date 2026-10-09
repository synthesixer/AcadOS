package com.project.acados.domain.entity;

import com.project.acados.domain.enums.SectionStatus;
import com.project.acados.state.ActiveSectionState;
import com.project.acados.state.CancelledSectionState;
import com.project.acados.state.SectionState;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

/**
 * Section entity representing course study groups.
 * Reference: database.md §2.5, class diagram.puml
 */
@Entity
@Table(
    name = "sections",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_sections_course_section_num", columnNames = {"course_id", "section_number"})
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Section {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @NotNull
    @Min(1)
    @Column(name = "section_number", nullable = false)
    private Integer sectionNumber;

    @NotNull
    @Min(1)
    @Column(name = "capacity", nullable = false)
    private Integer capacity;

    @NotNull
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SectionStatus status = SectionStatus.ACTIVE;

    /**
     * State Pattern: state object matching the stored status. Not a column.
     */
    @Transient
    private SectionState state;

    public void cancel() {
        getState().cancel(this);
    }

    public SectionState getState() {
        if (state == null) {
            state = status == SectionStatus.CANCELLED ? new CancelledSectionState() : new ActiveSectionState();
        }
        return state;
    }

    public void setState(SectionState state) {
        this.state = state;
    }

    public void setStatus(SectionStatus status) {
        this.status = status;
        this.state = null;
    }
}

