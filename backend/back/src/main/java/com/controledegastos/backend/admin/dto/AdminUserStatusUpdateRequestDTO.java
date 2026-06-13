package com.controledegastos.backend.admin.dto;

/**
 * Permite ativar ou suspender uma conta sem remover seus dados.
 */
public record AdminUserStatusUpdateRequestDTO(
        boolean active
) {
}
