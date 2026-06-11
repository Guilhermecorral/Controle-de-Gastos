package com.controledegastos.backend.transactions.DTO;

import java.time.LocalDateTime;

/**
 * Resume os metadados do anexo fiscal associado a uma transacao.
 */
public record TransactionReceiptSummaryDTO(
        String originalFilename,
        String contentType,
        Long sizeBytes,
        LocalDateTime uploadedAt
) {
}
