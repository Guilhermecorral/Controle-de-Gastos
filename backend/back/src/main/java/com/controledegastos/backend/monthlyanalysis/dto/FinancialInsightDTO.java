package com.controledegastos.backend.monthlyanalysis.dto;

import java.math.BigDecimal;

public record FinancialInsightDTO(
        String code,
        FinancialInsightSeverity severity,
        String title,
        String message,
        String evidence,
        BigDecimal suggestedAmount
) {
}
