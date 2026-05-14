package com.controledegastos.backend.wishlist.dto;

import com.controledegastos.backend.wishlist.WishlistItem;

import java.time.LocalDate;

/**
 * Representa os dados informados ao marcar um item como comprado.
 */
public record WishlistPurchaseRequestDTO(
        LocalDate purchaseDate,
        WishlistItem.PurchasePaymentMethod paymentMethod,
        Integer installments,
        Boolean firstInstallmentNextMonth
) {
}
