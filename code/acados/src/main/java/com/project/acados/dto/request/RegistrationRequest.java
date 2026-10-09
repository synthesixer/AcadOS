package com.project.acados.dto.request;

import jakarta.validation.constraints.NotNull;

/**
 * Request body of POST /api/v1/registrations.
 */
public record RegistrationRequest(
        @NotNull Long sectionId
) {
}
