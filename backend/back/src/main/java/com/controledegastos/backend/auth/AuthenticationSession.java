package com.controledegastos.backend.auth;

import com.controledegastos.backend.auth.dto.AuthResponseDTO;

/**
 * Agrupa os tokens emitidos no backend e o perfil publico devolvido ao frontend.
 */
public record AuthenticationSession(
        String accessToken,
        String refreshToken,
        AuthResponseDTO user
) {
}
