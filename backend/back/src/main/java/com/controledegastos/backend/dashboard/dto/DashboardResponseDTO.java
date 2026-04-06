package com.controledegastos.backend.dashboard.dto; // Declares the package for the main dashboard response.

import com.controledegastos.backend.transactions.DTO.TransactionResponseDTO; // Reuses the transaction response DTO for the recent transactions section.

import java.math.BigDecimal; // Imports the monetary type used in financial summaries.
import java.util.List; // Imports the list type used by the grouped and recent sections.

public record DashboardResponseDTO( // Defines the immutable payload returned by GET /api/dashboard.
        BigDecimal totalReceitas, // Represents how much money came in.
        BigDecimal totalDespesas, // Represents how much money went out.
        BigDecimal saldo, // Represents the difference between incomes and expenses.
        List<TransactionResponseDTO> ultimasTransacoes, // Represents the recent transaction feed shown on the dashboard.
        List<DashboardCategorySummaryDTO> gastosPorCategoria // Represents the grouped spending analysis by category.
) { // Closes the record declaration.
} // Closes the DTO type.
