package com.controledegastos.backend.transactions.DTO;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Agrupa as transacoes revisadas pelo usuario antes da confirmacao da importacao.
 */
public record TransactionImportRequestDTO(
        @NotEmpty(message = "Selecione ao menos uma transacao para importar")
        @Size(max = 1000, message = "A importacao suporta no maximo 1000 transacoes por vez")
        List<@Valid TransactionRequestDTO> transactions
) {
}
