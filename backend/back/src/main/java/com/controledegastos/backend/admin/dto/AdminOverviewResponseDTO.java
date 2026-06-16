package com.controledegastos.backend.admin.dto;

import java.math.BigDecimal;

/**
 * Resume a saude operacional do produto para o painel administrativo.
 */
public record AdminOverviewResponseDTO(
        long totalUsuarios,
        long usuariosAtivos,
        long administradores,
        long usuariosComDoisFatores,
        long emailsPermitidosParaAdmin,
        BigDecimal totalReceitas,
        BigDecimal totalDespesas,
        BigDecimal saldoGlobal,
        String statusApi
) {
}
