package com.controledegastos.backend.auth.dto;

/**
 * Mantém a resposta neutra da recuperação de senha e expõe o link apenas em desenvolvimento local.
 */
public record ForgotPasswordResponseDTO(
        String message,
        String debugResetLink
) {
}
