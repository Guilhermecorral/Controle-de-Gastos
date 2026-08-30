package com.controledegastos.backend.transactions.DTO;

public record ReceiptMatchCandidateDTO(Long transactionId, int score, String rationale) {
}
