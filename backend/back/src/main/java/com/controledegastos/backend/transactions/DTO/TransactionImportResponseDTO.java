package com.controledegastos.backend.transactions.DTO;

/**
 * Resume uma importacao concluida sem expor dados do arquivo de extrato original.
 */
public record TransactionImportResponseDTO(
        int importedTransactions,
        String message
) {
}
