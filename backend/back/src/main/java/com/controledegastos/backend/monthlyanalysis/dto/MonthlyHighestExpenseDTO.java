package com.controledegastos.backend.monthlyanalysis.dto; // Declares the package for highest-expense response objects.

import com.controledegastos.backend.transactions.Transaction; // Imports the transaction enums used by the DTO.

import java.math.BigDecimal; // Imports the exact numeric type used for money.
import java.time.LocalDate; // Imports the date type used by the transaction date.

public record MonthlyHighestExpenseDTO( // Declares the immutable DTO that exposes the biggest expense of the month.
        String description, // Stores the expense description shown to the user.
        BigDecimal amount, // Stores the amount of the biggest expense.
        Transaction.TransactionCategory category, // Stores the category of the biggest expense.
        LocalDate transactionDate // Stores the business date of the biggest expense.
) { // Closes the record declaration.
} // Closes the DTO type.
