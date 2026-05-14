package com.controledegastos.backend.monthlyanalysis.dto;

import java.math.BigDecimal;

public record YearToDateComparisonDTO(
        YearToDateSummaryDTO anoAtual,
        YearToDateSummaryDTO anoAnterior,
        BigDecimal diferencaReceitas,
        BigDecimal diferencaDespesas,
        BigDecimal diferencaSaldo,
        AnalysisTrend tendenciaReceitas,
        AnalysisTrend tendenciaDespesas,
        AnalysisTrend tendenciaSaldo,
        AnalysisTrend tendenciaGeral
) {
}
