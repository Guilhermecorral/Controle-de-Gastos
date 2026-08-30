package com.controledegastos.backend.ofxupload;

import com.controledegastos.backend.transactions.DTO.TransactionRequestDTO;
import com.controledegastos.backend.transactions.Transaction.PaymentMethod;
import com.controledegastos.backend.transactions.Transaction.TransactionCategory;
import com.controledegastos.backend.transactions.Transaction.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ImportPreviewTransactionDTO(
        TransactionType type,
        String description,
        TransactionCategory category,
        BigDecimal amount,
        PaymentMethod paymentMethod,
        Integer installments,
        LocalDate transactionDate,
        boolean selectedByDefault,
        ImportConfidence confidence,
        String source,
        String rationale
) {
    public static ImportPreviewTransactionDTO fromOFX(TransactionRequestDTO transaction, int index) {
        return new ImportPreviewTransactionDTO(
                transaction.type(),
                transaction.description(),
                transaction.category(),
                transaction.amount(),
                transaction.paymentMethod(),
                transaction.installments(),
                transaction.transactionDate(),
                true,
                ImportConfidence.ALTA,
                "OFX · transacao " + (index + 1),
                "Campos estruturados fornecidos pelo extrato bancario."
        );
    }
}
