package com.project.acados.dto.response;

import com.project.acados.domain.enums.UserRole;

public record UserResponse(Long id, String universityId, String fullName, String email, UserRole role) {
}
