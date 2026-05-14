package com.controledegastos.backend.transactions;

public interface TransactionCategorySummaryProjection {

    Transaction.TransactionCategory getCategory();

    java.math.BigDecimal getTotalAmount();
}
