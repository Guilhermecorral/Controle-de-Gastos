package com.controledegastos.backend.auth.dto;

/**
 * Informa ao frontend que a senha foi validada, mas o segundo fator ainda precisa ser confirmado.
 */
public record TwoFactorChallengeResponseDTO(
        boolean requiresTwoFactor,
        String message
) {
}
