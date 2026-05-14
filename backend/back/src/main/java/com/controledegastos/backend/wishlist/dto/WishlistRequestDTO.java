package com.controledegastos.backend.wishlist.dto;

import com.controledegastos.backend.wishlist.WishlistItem;

import java.math.BigDecimal;

/**
 * Representa os dados usados para criar ou editar um item da wishlist.
 */
public record WishlistRequestDTO(
        String description,
        BigDecimal originalPrice,
        BigDecimal discountPercent,
        WishlistItem.Priority priority,
        WishlistItem.WishlistCategory category,
        String notes
) {
}
