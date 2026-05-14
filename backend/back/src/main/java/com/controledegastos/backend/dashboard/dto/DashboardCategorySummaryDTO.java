package com.controledegastos.backend.dashboard.dto;

import com.controledegastos.backend.transactions.Transaction;

import java.math.BigDecimal;

/**
 * Resume o total gasto por categoria exibido no dashboard.
 */
public record DashboardCategorySummaryDTO(
        Transaction.TransactionCategory category,
        BigDecimal totalAmount
) {
}
