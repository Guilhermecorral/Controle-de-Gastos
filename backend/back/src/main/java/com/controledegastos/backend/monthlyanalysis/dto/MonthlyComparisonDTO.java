package com.controledegastos.backend.monthlyanalysis.dto; // Declares the package for monthly comparison response objects.

import java.math.BigDecimal; // Imports the exact numeric type used for money.

public record MonthlyComparisonDTO( // Declares the immutable DTO used to summarize the previous month.
        BigDecimal totalReceitas, // Stores the total income of the previous month.
        BigDecimal totalDespesas, // Stores the total expenses of the previous month.
        BigDecimal saldo // Stores the calculated balance of the previous month.
) { // Closes the record declaration.
} // Closes the DTO type.
