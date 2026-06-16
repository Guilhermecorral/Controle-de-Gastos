package com.controledegastos.backend.admin.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Resume a saude operacional do produto para o painel administrativo.
 */
public record AdminOverviewResponseDTO(
        long totalUsuarios,
        long usuariosAtivos,
        long administradores,
        long usuariosComDoisFatores,
        long emailsPermitidosParaAdmin,
        List<String> adminWhitelist,
        BigDecimal totalReceitas,
        BigDecimal totalDespesas,
        BigDecimal saldoGlobal,
        String statusApi
) {
}
