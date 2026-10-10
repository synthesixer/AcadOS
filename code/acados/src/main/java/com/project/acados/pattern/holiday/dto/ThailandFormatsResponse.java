package com.project.acados.pattern.holiday.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Data transfer object mapping the root JSON response from ThailandFormats API.
 * Endpoint: https://thailandformats.com/api/v1/holidays/{year}
 * Reference: Implement_Plan-AcadOS.md §4, §10.3, §14.5
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class ThailandFormatsResponse {

    private Integer year;
    private Integer count;

    @Builder.Default
    private List<ThailandFormatsHolidayDto> holidays = new ArrayList<>();
}

