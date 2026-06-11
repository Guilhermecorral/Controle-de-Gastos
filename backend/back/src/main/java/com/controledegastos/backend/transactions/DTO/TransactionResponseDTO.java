package com.controledegastos.backend.transactions.DTO;

import com.controledegastos.backend.transactions.Transaction.PaymentMethod;
import com.controledegastos.backend.transactions.Transaction.TransactionCategory;
import com.controledegastos.backend.transactions.Transaction.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Representa os dados devolvidos ao cliente apos ler uma transacao.
 */
public record TransactionResponseDTO(
        Long id,
        TransactionType type,
        String description,
        TransactionCategory category,
        BigDecimal amount,
        PaymentMethod paymentMethod,
        Integer installments,
        LocalDate transactionDate,
        LocalDateTime createdAt,
        TransactionReceiptSummaryDTO receipt
) {
}
