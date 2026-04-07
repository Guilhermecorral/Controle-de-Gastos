package com.controledegastos.backend.monthlyanalysis.dto; // Declares the package for the monthly analysis response payload.

import com.controledegastos.backend.dashboard.dto.DashboardCategorySummaryDTO; // Reuses the grouped category DTO already adopted by the dashboard.

import java.math.BigDecimal; // Imports the exact numeric type used for money.
import java.util.List; // Imports the list type used by grouped categories.

public record MonthlyAnalysisResponseDTO( // Declares the immutable payload returned by the monthly analysis endpoint.
        int year, // Stores the requested analysis year.
        int month, // Stores the requested analysis month.
        BigDecimal totalReceitas, // Stores the total income of the selected month.
        BigDecimal totalDespesas, // Stores the total expenses of the selected month.
        BigDecimal saldo, // Stores the balance calculated from income minus expenses.
        MonthlyHighestExpenseDTO maiorGasto, // Stores the biggest expense of the selected month, or null when there is none.
        List<DashboardCategorySummaryDTO> gastosPorCategoria, // Stores the grouped expenses by category for the selected month.
        MonthlyComparisonDTO comparativoMesAnterior // Stores the comparison snapshot of the previous month.
) { // Closes the record declaration.
} // Closes the DTO type.
