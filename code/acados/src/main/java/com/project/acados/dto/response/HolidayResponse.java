package com.project.acados.dto.response;

import com.project.acados.domain.entity.PublicHoliday;
import lombok.*;

import java.time.LocalDate;

/**
 * Response DTO for PublicHoliday entity.
 * Reference: class diagram.puml, Implement_Plan-AcadOS.md §16
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HolidayResponse {

    private Long id;
    private LocalDate date;
    private String name;
    private String description;

    public static HolidayResponse fromEntity(PublicHoliday holiday) {
        if (holiday == null) {
            return null;
        }
        return HolidayResponse.builder()
                .id(holiday.getId())
                .date(holiday.getDate())
                .name(holiday.getName())
                .description(holiday.getDescription())
                .build();
    }
}

