package com.controledegastos.backend.tax;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PaymentConfirmationRequest(
        @NotNull @DecimalMin("0.01") @Digits(integer = 17, fraction = 2) BigDecimal paidAmount,
        @NotNull LocalDate paidDate,
        @NotBlank @Size(max = 255) String accountDescription,
        @Size(max = 255) String receiptReference
) {}
