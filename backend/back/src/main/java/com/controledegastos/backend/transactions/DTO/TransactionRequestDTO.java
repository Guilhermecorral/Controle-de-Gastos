package com.controledegastos.backend.transactions.DTO;

import com.controledegastos.backend.transactions.Transaction.PaymentMethod;
import com.controledegastos.backend.transactions.Transaction.TransactionCategory;
import com.controledegastos.backend.transactions.Transaction.TransactionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Representa os dados enviados para criar ou editar uma transacao.
 */
public record TransactionRequestDTO(
        @NotNull(message = "Tipo e obrigatorio")
        TransactionType type,

        @NotBlank(message = "Descricao e obrigatoria")
        @Size(max = 255, message = "Descricao deve ter no maximo 255 caracteres")
        String description,

        @NotNull(message = "Categoria e obrigatoria")
        TransactionCategory category,

        @NotNull(message = "Valor e obrigatorio")
        @DecimalMin(value = "0.01", message = "Valor deve ser maior que zero")
        BigDecimal amount,

        @NotNull(message = "Forma de pagamento e obrigatoria")
        PaymentMethod paymentMethod,

        @Min(value = 1, message = "Parcelas deve ser no minimo 1")
        Integer installments,

        @NotNull(message = "Data e obrigatoria")
        LocalDate transactionDate
) {
}
