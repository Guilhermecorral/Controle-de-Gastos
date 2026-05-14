package com.controledegastos.backend.dashboard.dto;

import com.controledegastos.backend.transactions.DTO.TransactionResponseDTO;

import java.math.BigDecimal;
import java.util.List;

/**
 * Representa a resposta completa do dashboard.
 */
public record DashboardResponseDTO(
        BigDecimal totalReceitas,
        BigDecimal totalDespesas,
        BigDecimal saldo,
        BigDecimal totalReceitasAcumuladas,
        BigDecimal totalDespesasAcumuladas,
        BigDecimal saldoAcumulado,
        BigDecimal receitasAnoReferencia,
        BigDecimal despesasAnoReferencia,
        BigDecimal resultadoAnoReferencia,
        BigDecimal receitasMesAtual,
        BigDecimal despesasMesAtual,
        BigDecimal resultadoMesAtual,
        int anoReferencia,
        int mesReferencia,
        List<TransactionResponseDTO> ultimasTransacoes,
        List<DashboardCategorySummaryDTO> gastosPorCategoria
) {
}
