package com.project.acados.pattern.holiday.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

import java.time.LocalDate;

/**
 * Data transfer object mapping JSON responses from Nager.Date Public Holiday API.
 * Reference: Implement_Plan-AcadOS.md §4, §10.3, §14.5
 * Endpoint: https://date.nager.at/api/v3/publicholidays/{year}/TH
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class NagerHolidayDto {

    private LocalDate date;
    private String localName;
    private String name;
    private String countryCode;
    private Boolean fixed;
    private Boolean global;
}

