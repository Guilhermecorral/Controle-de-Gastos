package com.controledegastos.backend.wishlist.dto;

import com.controledegastos.backend.wishlist.WishlistItem;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Representa os dados devolvidos pelos endpoints da wishlist.
 */
public record WishlistResponseDTO(
        Long id,
        String description,
        BigDecimal originalPrice,
        BigDecimal discountPercent,
        BigDecimal finalPrice,
        WishlistItem.Priority priority,
        WishlistItem.WishlistCategory category,
        String notes,
        WishlistItem.WishlistStatus status,
        LocalDate purchaseDate,
        WishlistItem.PurchasePaymentMethod paymentMethod,
        Integer installments,
        Boolean firstInstallmentNextMonth,
        Boolean archivedAfterPurchase,
        Long listId,
        String listName,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
