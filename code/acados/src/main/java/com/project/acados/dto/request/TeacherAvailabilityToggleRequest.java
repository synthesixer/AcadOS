package com.project.acados.dto.request;

import jakarta.validation.constraints.NotNull;

public record TeacherAvailabilityToggleRequest(
        @NotNull(message = "กรุณาระบุรหัสช่วงเวลา (TimeSlot ID)")
        Long timeSlotId,

        @NotNull(message = "กรุณาระบุสถานะความสะดวก (isAvailable)")
        Boolean isAvailable
) {}

