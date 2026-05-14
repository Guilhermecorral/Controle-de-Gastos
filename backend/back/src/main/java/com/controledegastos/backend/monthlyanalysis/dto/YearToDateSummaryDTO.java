package com.controledegastos.backend.monthlyanalysis.dto;

import java.math.BigDecimal;

public record YearToDateSummaryDTO(
        int year,
        int monthLimit,
        BigDecimal totalReceitas,
        BigDecimal totalDespesas,
        BigDecimal saldo
) {
}
