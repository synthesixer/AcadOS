package com.project.acados.dto.request;

import jakarta.validation.constraints.NotNull;

public record SwapCreateRequest(
        @NotNull Long requestingScheduleId,
        @NotNull Long targetTeacherId,
        @NotNull Long targetScheduleId
) {
}
