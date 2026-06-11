package com.controledegastos.backend.transactions.DTO;

import com.controledegastos.backend.transactions.Transaction.PaymentMethod;
import com.controledegastos.backend.transactions.Transaction.TransactionCategory;
import com.controledegastos.backend.transactions.Transaction.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Representa um anexo fiscal ja salvo e pronto para consulta por periodo.
 */
public record TransactionReceiptResponseDTO(
        Long transactionId,
        TransactionType type,
        String description,
        TransactionCategory category,
        BigDecimal amount,
        PaymentMethod paymentMethod,
        Integer installments,
        LocalDate transactionDate,
        String originalFilename,
        String contentType,
        Long sizeBytes,
        LocalDateTime uploadedAt,
        Integer coveredTransactions
) {
}
