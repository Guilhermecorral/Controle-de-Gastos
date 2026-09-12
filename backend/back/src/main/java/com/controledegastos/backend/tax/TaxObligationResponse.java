package com.controledegastos.backend.tax;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record TaxObligationResponse(
        UUID id,
        String name,
        String issuingAuthority,
        TaxObligation.Category category,
        LocalDate dueDate,
        BigDecimal estimatedAmount,
        BigDecimal paidAmount,
        LocalDate paidDate,
        TaxObligation.Status status,
        String statusDisplay,
        TaxObligation.DocumentStage documentStage,
        TaxObligation.Recurrence recurrence,
        Integer competenceYear,
        Integer competenceMonth,
        String notes,
        TaxObligation.Origin origin,
        String paymentAccountDescription,
        String receiptReference,
        Long linkedTransactionId,
        Long linkedDarfId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
