package com.controledegastos.backend.admin.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Recebe a nova role desejada para a conta administrada.
 */
public record AdminUserRoleUpdateRequestDTO(
        @NotBlank(message = "Role e obrigatoria")
        String role
) {
}
