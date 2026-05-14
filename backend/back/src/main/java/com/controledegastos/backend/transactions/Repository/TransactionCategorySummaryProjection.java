package com.controledegastos.backend.transactions.Repository;

import com.controledegastos.backend.transactions.Transaction;

public interface TransactionCategorySummaryProjection {

    Transaction.TransactionCategory getCategory();

    java.math.BigDecimal getTotalAmount();
}
