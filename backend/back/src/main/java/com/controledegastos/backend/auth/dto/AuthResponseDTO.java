package com.controledegastos.backend.auth.dto;

/**
 * Representa o usuario autenticado devolvido apos criar ou renovar uma sessao.
 */
public record AuthResponseDTO(
        String name,
        String email,
        String role,
        boolean twoFactorEnabled
) {
}
