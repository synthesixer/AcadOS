package com.project.acados.pattern.holiday.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.time.LocalDate;

/**
 * Data transfer object mapping an individual holiday item from ThailandFormats API.
 * Endpoint: https://thailandformats.com/api/v1/holidays/{year}
 * Reference: Implement_Plan-AcadOS.md §4, §10.3, §14.5
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class ThailandFormatsHolidayDto {

    private String title;

    @JsonProperty("start_date")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    @JsonProperty("end_date")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;

    private String type;

    @JsonProperty("alcohol_ban")
    private Boolean alcoholBan;

    private String details;

    private String slug;
}

