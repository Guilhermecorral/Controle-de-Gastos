package com.controledegastos.backend.monthlyanalysis.dto;

import com.controledegastos.backend.dashboard.dto.DashboardCategorySummaryDTO;

import java.math.BigDecimal;
import java.util.List;

public record MonthlyAnalysisResponseDTO(
        int year,
        int month,
        BigDecimal totalReceitas,
        BigDecimal totalDespesas,
        BigDecimal saldo,
        MonthlyHighestExpenseDTO maiorGasto,
        List<DashboardCategorySummaryDTO> gastosPorCategoria,
        MonthlyComparisonDTO comparativoMesAnterior,
        MonthlyComparisonDTO comparativoMesmoMesAnoAnterior,
        YearToDateSummaryDTO acumuladoAnoAtual,
        YearToDateComparisonDTO comparativoAcumuladoAnoAnterior
) {
}
