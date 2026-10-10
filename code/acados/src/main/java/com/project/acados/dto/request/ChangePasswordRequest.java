package com.project.acados.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @NotBlank(message = "กรุณาระบุรหัสผ่านปัจจุบัน")
        String currentPassword,

        @NotBlank(message = "กรุณาระบุรหัสผ่านใหม่")
        @Size(min = 6, message = "รหัสผ่านใหม่ต้องมีความยาวอย่างน้อย 6 ตัวอักษร")
        String newPassword
) {}

