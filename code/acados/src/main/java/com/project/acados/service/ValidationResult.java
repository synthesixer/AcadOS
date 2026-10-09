package com.project.acados.service;

import lombok.*;

/**
 * Result of hard constraint evaluation for a candidate schedule.
 * Contains boolean passed status and failure reason if rejected.
 * Reference: class diagram.puml (§6 scheduling), Implement_Plan-AcadOS.md §10.3, §12.3
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class ValidationResult {

    private boolean passed;
    private String reason;

    public static ValidationResult pass() {
        return ValidationResult.builder()
                .passed(true)
                .reason(null)
                .build();
    }

    public static ValidationResult fail(String reason) {
        return ValidationResult.builder()
                .passed(false)
                .reason(reason)
                .build();
    }
}

