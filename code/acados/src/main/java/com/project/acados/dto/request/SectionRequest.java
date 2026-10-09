package com.project.acados.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Request body of POST /api/v1/sections and PUT /api/v1/sections/{id}.
 * Creating a section needs every field (validation group OnCreate);
 * updating a section changes the capacity only, so courseId and sectionNumber are ignored.
 * Reference: class diagram.puml (SectionService), Userflow A12-2, A12-3
 */
public record SectionRequest(
        @NotNull(groups = OnCreate.class) Long courseId,
        @NotNull(groups = OnCreate.class) @Min(1) Integer sectionNumber,
        @NotNull @Min(1) Integer capacity
) {

    /**
     * Bean Validation group for the fields that are required only when creating a section.
     */
    public interface OnCreate {
    }
}
