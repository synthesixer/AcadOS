package com.project.acados.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record TeacherPreferenceRequest(
        @NotNull(message = "กรุณาระบุรายวิชา")
        Long courseId,

        @NotNull(message = "กรุณาระบุลำดับความสำคัญ (1-5)")
        @Min(value = 1, message = "ลำดับความสำคัญต้องอยู่ระหว่าง 1 ถึง 5")
        @Max(value = 5, message = "ลำดับความสำคัญต้องอยู่ระหว่าง 1 ถึง 5")
        Integer priority
) {}
