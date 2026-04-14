package com.controledegastos.backend.monthlyanalysis.dto; // Declares the package for year-to-date response objects.

import java.math.BigDecimal; // Imports the exact numeric type used for money.

public record YearToDateSummaryDTO( // Declares the immutable DTO used to summarize a year-to-date snapshot.
        int year, // Stores the year of the accumulated period.
        int monthLimit, // Stores the last month included in the accumulation.
        BigDecimal totalReceitas, // Stores the income total accumulated from January to the informed month.
        BigDecimal totalDespesas, // Stores the expense total accumulated from January to the informed month.
        BigDecimal saldo // Stores the accumulated balance of the informed year-to-date interval.
) { // Closes the record declaration.
} // Closes the DTO type.
