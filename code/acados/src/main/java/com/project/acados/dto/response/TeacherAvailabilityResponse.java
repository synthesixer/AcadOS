package com.project.acados.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.DayOfWeek;
import java.time.LocalTime;

public record TeacherAvailabilityResponse(
        Long id,
        Long timeSlotId,
        DayOfWeek dayOfWeek,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm:ss")
        LocalTime startTime,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm:ss")
        LocalTime endTime,
        boolean isAvailable
) {}

