package com.controledegastos.backend.admin.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entrega a leitura administrativa consolidada de cada conta do sistema.
 */
public record AdminUserResponseDTO(
        Long id,
        String name,
        String email,
        String role,
        boolean active,
        boolean twoFactorEnabled,
        boolean adminPromotionAllowed,
        boolean protectedAdmin,
        boolean currentSessionUser,
        LocalDateTime createdAt,
        LocalDateTime suspendedAt,
        long totalTransactions,
        LocalDate lastTransactionDate
) {
}
