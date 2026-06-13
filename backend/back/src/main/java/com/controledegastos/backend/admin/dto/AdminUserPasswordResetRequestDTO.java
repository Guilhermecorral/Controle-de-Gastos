package com.controledegastos.backend.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Recebe a nova senha definida manualmente pelo administrador.
 */
public record AdminUserPasswordResetRequestDTO(
        @NotBlank(message = "Nova senha e obrigatoria")
        @Size(min = 8, max = 100, message = "Senha deve ter entre 8 e 100 caracteres")
        String newPassword
) {
}
