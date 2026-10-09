package com.project.acados.domain.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDate;

/**
 * PublicHoliday entity representing official holidays synced from external API.
 * Reference: database.md §2.16, class diagram.puml
 */
@Entity
@Table(
    name = "public_holidays",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_public_holidays_date_name", columnNames = {"holiday_date", "name"})
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PublicHoliday {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(name = "holiday_date", nullable = false)
    private LocalDate date;

    @NotBlank
    @Size(max = 200)
    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Size(max = 500)
    @Column(name = "description", length = 500)
    private String description;
}

