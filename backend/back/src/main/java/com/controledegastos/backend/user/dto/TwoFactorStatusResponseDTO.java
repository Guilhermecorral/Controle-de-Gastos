package com.controledegastos.backend.user.dto;

/**
 * Resume o estado atual da autenticacao em dois fatores para a tela de configuracoes.
 */
public record TwoFactorStatusResponseDTO(
        boolean enabled,
        boolean pendingSetup,
        String issuer
) {
}
