package com.controledegastos.backend.dashboard.dto; // Declares the package for the main dashboard response.

import com.controledegastos.backend.transactions.DTO.TransactionResponseDTO; // Reuses the transaction response DTO for the recent transactions section.

import java.math.BigDecimal; // Imports the monetary type used in financial summaries.
import java.util.List; // Imports the list type used by the grouped and recent sections.

public record DashboardResponseDTO( // Defines the immutable payload returned by GET /api/dashboard.
        BigDecimal totalReceitas, // Keeps backward compatibility and now mirrors the accumulated income total.
        BigDecimal totalDespesas, // Keeps backward compatibility and now mirrors the accumulated expense total.
        BigDecimal saldo, // Keeps backward compatibility and now mirrors the accumulated balance.
        BigDecimal totalReceitasAcumuladas, // Represents all income accumulated up to the current moment.
        BigDecimal totalDespesasAcumuladas, // Represents all expenses accumulated up to the current moment.
        BigDecimal saldoAcumulado, // Represents the historical balance carried from month to month.
        BigDecimal receitasAnoReferencia, // Represents the income accumulated from January up to the selected reference month.
        BigDecimal despesasAnoReferencia, // Represents the expenses accumulated from January up to the selected reference month.
        BigDecimal resultadoAnoReferencia, // Represents the year-to-date result up to the selected reference month.
        BigDecimal receitasMesAtual, // Represents the income that belongs only to the current month.
        BigDecimal despesasMesAtual, // Represents the expenses that belong only to the current month.
        BigDecimal resultadoMesAtual, // Represents the result of the current month without mixing previous months.
        int anoReferencia, // Represents the year used as the current-month reference of the dashboard.
        int mesReferencia, // Represents the month used as the current-month reference of the dashboard.
        List<TransactionResponseDTO> ultimasTransacoes, // Represents the recent transaction feed shown on the dashboard.
        List<DashboardCategorySummaryDTO> gastosPorCategoria // Represents the grouped spending analysis of the current month.
) { // Closes the record declaration.
} // Closes the DTO type.
