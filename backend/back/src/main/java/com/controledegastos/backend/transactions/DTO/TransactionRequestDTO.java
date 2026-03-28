package com.controledegastos.backend.transactions.DTO;

import com.controledegastos.backend.transactions.Transaction.TransactionCategory;
import com.controledegastos.backend.transactions.Transaction.TransactionType;
import com.controledegastos.backend.transactions.Transaction.PaymentMethod;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;


public record TransactionRequestDTO(

        // Mandatory type - RECEITA ou DESPESA
        @NotNull(message = "Tipo é obrigatório")
        TransactionType type,

        // Mandatory description - ex.: Burger King
        @NotBlank(message = "Descrição é obrigatória")
        @Size(max = 255, message = "Descrição deve ter no máximo 255 caracteres")
        String description,

        // Mandatory category
        @NotNull(message = "Categoria é obrigatória")
        TransactionCategory category,

        // Value - today positive never negative
        @NotNull(message = "Valor é obrigatório")
        @DecimalMin(value = "0.01", message = "Valor deve ser maior que zero")
        BigDecimal amount,

        // Mandatory payment method
        @NotNull(message = "Forma de pagamento é obrigatória")
        PaymentMethod paymentMethod,

        // Transaction date - cannot be future
        @NotNull(message = "Data é obrigatória")
        LocalDate transactionDate

) {}
