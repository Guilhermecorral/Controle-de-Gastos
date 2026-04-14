package com.controledegastos.backend.wishlist.dto; // Declares the package for wishlist purchase DTOs.

import com.controledegastos.backend.wishlist.WishlistItem; // Imports the payment enum reused by the request DTO.

import java.time.LocalDate; // Imports the date type used by the purchase request.

public record WishlistPurchaseRequestDTO( // Declares the immutable DTO used when a wishlist item is marked as purchased.
        LocalDate purchaseDate, // Stores the date when the purchase happened.
        WishlistItem.PurchasePaymentMethod paymentMethod, // Stores the payment method chosen for the purchase.
        Integer installments, // Stores how many installments were chosen when the payment is parcelled.
        Boolean firstInstallmentNextMonth // Stores whether the first installment should start only in the next month.
) { // Closes the record declaration.
} // Closes the DTO type.
