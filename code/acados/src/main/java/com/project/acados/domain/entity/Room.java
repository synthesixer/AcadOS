package com.project.acados.domain.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * Room entity representing classrooms and exam rooms.
 * Reference: database.md §2.6, class diagram.puml
 */
@Entity
@Table(
    name = "rooms",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_rooms_building_number", columnNames = {"building", "room_number"})
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 20)
    @Column(name = "room_number", nullable = false, length = 20)
    private String roomNumber;

    @NotBlank
    @Size(max = 100)
    @Column(name = "building", nullable = false, length = 100)
    private String building;

    @NotNull
    @Column(name = "floor", nullable = false)
    private Integer floor;

    @NotNull
    @Min(1)
    @Column(name = "capacity", nullable = false)
    private Integer capacity;

    @NotNull
    @Builder.Default
    @Column(name = "is_available", nullable = false)
    private Boolean isAvailable = true;
}

