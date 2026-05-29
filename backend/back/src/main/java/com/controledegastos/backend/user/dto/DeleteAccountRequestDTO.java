package com.controledegastos.backend.user.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Exige a senha atual antes de apagar a conta em definitivo.
 */
public record DeleteAccountRequestDTO(
        @NotBlank(message = "Senha atual é obrigatória")
        String password
) {
}
