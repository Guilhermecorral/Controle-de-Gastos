package com.controledegastos.backend.user.dto;

/**
 * Entrega os dados necessarios para cadastrar a conta em um app autenticador.
 */
public record TwoFactorSetupResponseDTO(
        String secret,
        String issuer,
        String accountName,
        String otpAuthUri
) {
}
