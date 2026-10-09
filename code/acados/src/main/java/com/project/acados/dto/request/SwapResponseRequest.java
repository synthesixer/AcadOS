package com.project.acados.dto.request;

import jakarta.validation.constraints.NotNull;

/** The boolean matches TeacherSwapService.respondSwap(..., boolean accept). */
public record SwapResponseRequest(@NotNull Boolean accept) {
}
