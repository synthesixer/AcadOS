package com.project.acados.dto.request;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank String universityId,
        @NotBlank String password
) {
}
