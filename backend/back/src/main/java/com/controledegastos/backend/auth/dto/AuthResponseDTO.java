package com.controledegastos.backend.auth.dto;

/**
 * Representa os tokens retornados apos autenticacao bem-sucedida.
 */
public record AuthResponseDTO(
        String accessToken,
        String refreshToken,
        String name,
        String email,
        String role
) {
}
