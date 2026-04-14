package com.controledegastos.backend.monthlyanalysis.dto; // Declares the package for monthly comparison response objects.

import java.math.BigDecimal; // Imports the exact numeric type used for money.

public record MonthlyComparisonDTO( // Declares the immutable DTO used to compare the selected month with another month.
        int year, // Stores the year of the compared month.
        int month, // Stores the month number of the compared month.
        BigDecimal totalReceitas, // Stores the total income of the compared month.
        BigDecimal totalDespesas, // Stores the total expenses of the compared month.
        BigDecimal saldo, // Stores the calculated balance of the compared month.
        BigDecimal diferencaReceitas, // Stores the income difference between the selected month and the compared month.
        BigDecimal diferencaDespesas, // Stores the expense difference between the selected month and the compared month.
        BigDecimal diferencaSaldo, // Stores the balance difference between the selected month and the compared month.
        AnalysisTrend tendenciaReceitas, // Stores the trend of the income comparison.
        AnalysisTrend tendenciaDespesas, // Stores the trend of the expense comparison.
        AnalysisTrend tendenciaSaldo, // Stores the trend of the balance comparison.
        AnalysisTrend tendenciaGeral // Stores the overall interpretation of the month comparison using the balance as the main signal.
) { // Closes the record declaration.
} // Closes the DTO type.
