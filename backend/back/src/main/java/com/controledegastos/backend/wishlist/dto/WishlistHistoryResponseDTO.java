package com.controledegastos.backend.wishlist.dto;

import com.controledegastos.backend.wishlist.WishlistHistoryEntry;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Representa um evento do historico de alteracoes de um item da wishlist.
 */
public record WishlistHistoryResponseDTO(
        Long id,
        WishlistHistoryEntry.ActionType actionType,
        String description,
        BigDecimal finalPriceSnapshot,
        String listNameSnapshot,
        LocalDateTime createdAt
) {
}
