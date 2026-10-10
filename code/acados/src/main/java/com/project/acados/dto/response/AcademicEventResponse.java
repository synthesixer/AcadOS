package com.project.acados.dto.response;

import com.project.acados.domain.entity.AcademicEvent;
import com.project.acados.domain.enums.AcademicEventType;
import lombok.*;

import java.time.LocalDate;

/**
 * Response DTO for AcademicEvent entity.
 * Reference: class diagram.puml, Implement_Plan-AcadOS.md §16
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AcademicEventResponse {

    private Long id;
    private String eventName;
    private AcademicEventType eventType;
    private LocalDate startDate;
    private LocalDate endDate;

    public static AcademicEventResponse fromEntity(AcademicEvent event) {
        if (event == null) {
            return null;
        }
        return AcademicEventResponse.builder()
                .id(event.getId())
                .eventName(event.getEventName())
                .eventType(event.getEventType())
                .startDate(event.getStartDate())
                .endDate(event.getEndDate())
                .build();
    }
}

