package com.controledegastos.backend.auth.dto;

/**
 * Mantém a resposta neutra da recuperação de senha para não revelar se um e-mail existe.
 */
public record ForgotPasswordResponseDTO(
        String message
) {
}
