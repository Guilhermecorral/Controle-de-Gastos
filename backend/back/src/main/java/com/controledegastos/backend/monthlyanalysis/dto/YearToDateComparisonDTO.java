package com.controledegastos.backend.monthlyanalysis.dto; // Declares the package for year-to-date comparison response objects.

import java.math.BigDecimal; // Imports the exact numeric type used for money.

public record YearToDateComparisonDTO( // Declares the immutable DTO used to compare year-to-date snapshots.
        YearToDateSummaryDTO anoAtual, // Stores the selected year-to-date snapshot.
        YearToDateSummaryDTO anoAnterior, // Stores the previous-year snapshot limited to the same month.
        BigDecimal diferencaReceitas, // Stores the income difference between the selected year and the previous one.
        BigDecimal diferencaDespesas, // Stores the expense difference between the selected year and the previous one.
        BigDecimal diferencaSaldo, // Stores the balance difference between the selected year and the previous one.
        AnalysisTrend tendenciaReceitas, // Stores the trend of the income comparison.
        AnalysisTrend tendenciaDespesas, // Stores the trend of the expense comparison.
        AnalysisTrend tendenciaSaldo, // Stores the trend of the balance comparison.
        AnalysisTrend tendenciaGeral // Stores the overall interpretation of the year-to-date comparison using the balance as the main signal.
) { // Closes the record declaration.
} // Closes the DTO type.
