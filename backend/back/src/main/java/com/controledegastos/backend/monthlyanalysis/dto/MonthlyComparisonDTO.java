package com.controledegastos.backend.monthlyanalysis.dto;

import java.math.BigDecimal;

public record MonthlyComparisonDTO(
        int year,
        int month,
        BigDecimal totalReceitas,
        BigDecimal totalDespesas,
        BigDecimal saldo,
        BigDecimal diferencaReceitas,
        BigDecimal diferencaDespesas,
        BigDecimal diferencaSaldo,
        AnalysisTrend tendenciaReceitas,
        AnalysisTrend tendenciaDespesas,
        AnalysisTrend tendenciaSaldo,
        AnalysisTrend tendenciaGeral
) {
}
