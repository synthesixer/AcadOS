package com.project.acados.dto.request;

import com.project.acados.domain.enums.AcademicEventType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDate;

/**
 * Request DTO for creating or updating an AcademicEvent.
 * Reference: class diagram.puml, Implement_Plan-AcadOS.md §16, database.md §2.15
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AcademicEventRequest {

    @NotBlank(message = "Event name is required")
    @Size(max = 200, message = "Event name must not exceed 200 characters")
    private String eventName;

    @NotNull(message = "Event type is required")
    private AcademicEventType eventType;

    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    @NotNull(message = "End date is required")
    private LocalDate endDate;
}

