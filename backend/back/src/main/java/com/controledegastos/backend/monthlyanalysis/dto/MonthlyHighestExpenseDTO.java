package com.controledegastos.backend.monthlyanalysis.dto;

import com.controledegastos.backend.transactions.Transaction;

import java.math.BigDecimal;
import java.time.LocalDate;

public record MonthlyHighestExpenseDTO(
        String description,
        BigDecimal amount,
        Transaction.TransactionCategory category,
        LocalDate transactionDate
) {
}
