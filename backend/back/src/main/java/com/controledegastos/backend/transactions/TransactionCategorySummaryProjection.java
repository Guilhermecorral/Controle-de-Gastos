package com.controledegastos.backend.transactions;

// Projection used to read expense totals grouped by category without loading full entities.
public interface TransactionCategorySummaryProjection {

    // Returns the transaction category that was grouped in the query.
    Transaction.TransactionCategory getCategory();

    // Returns the summed expense amount for the category.
    java.math.BigDecimal getTotalAmount();
}
