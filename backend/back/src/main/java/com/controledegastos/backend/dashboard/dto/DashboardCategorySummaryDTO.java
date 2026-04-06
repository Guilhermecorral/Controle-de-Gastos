package com.controledegastos.backend.dashboard.dto; // Declares the package for dashboard category summary responses.

import com.controledegastos.backend.transactions.Transaction; // Imports the enum owner so the DTO can expose the category type safely.

import java.math.BigDecimal; // Imports the precise numeric type used for money.

public record DashboardCategorySummaryDTO( // Defines an immutable DTO for one grouped expense category.
        Transaction.TransactionCategory category, // Exposes which category was grouped in the dashboard.
        BigDecimal totalAmount // Exposes the total amount spent in that category.
) { // Closes the record declaration.
} // Closes the DTO type.
