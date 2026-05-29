package com.controledegastos.backend.auth.dto;

/**
 * Padroniza respostas curtas para fluxos que nao precisam devolver dados complexos.
 */
public record SimpleMessageResponseDTO(String message) {
}
