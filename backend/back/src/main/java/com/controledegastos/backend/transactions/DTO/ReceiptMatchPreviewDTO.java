package com.controledegastos.backend.transactions.DTO;

import java.util.List;

public record ReceiptMatchPreviewDTO(
        int fileIndex,
        String filename,
        String confidence,
        List<ReceiptMatchCandidateDTO> candidates,
        String rationale
) {
}
