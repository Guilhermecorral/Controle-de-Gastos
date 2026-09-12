package com.controledegastos.backend.tax;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TaxObligationRequest(
        @NotBlank @Size(max = 160) String name,
        @Size(max = 120) String issuingAuthority,
        @NotNull TaxObligation.Category category,
        @NotNull LocalDate dueDate,
        @NotNull @DecimalMin("0.00") @Digits(integer = 17, fraction = 2) BigDecimal estimatedAmount,
        @NotNull TaxObligation.Recurrence recurrence,
        @NotNull @Min(1900) @Max(2200) Integer competenceYear,
        @Min(1) @Max(12) Integer competenceMonth,
        @Size(max = 1000) String notes,
        TaxObligation.Origin origin,
        TaxObligation.Status status,
        TaxObligation.DocumentStage documentStage
) {}
