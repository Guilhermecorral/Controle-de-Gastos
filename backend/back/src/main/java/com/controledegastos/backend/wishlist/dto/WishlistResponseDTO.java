package com.controledegastos.backend.wishlist.dto; // Declares the package for wishlist read DTOs.

import com.controledegastos.backend.wishlist.WishlistItem; // Imports the enums reused by the response DTO.

import java.math.BigDecimal; // Imports the money type used by price fields.
import java.time.LocalDate; // Imports the date type used by the purchase date.
import java.time.LocalDateTime; // Imports the date-time type used by audit fields.

public record WishlistResponseDTO( // Declares the immutable DTO returned by the wishlist endpoints.
        Long id, // Stores the database identifier of the item.
        String description, // Stores the visible name/description of the item.
        BigDecimal originalPrice, // Stores the original price informed by the user.
        BigDecimal discountPercent, // Stores the discount percentage applied to the item.
        BigDecimal finalPrice, // Stores the calculated final price after discount.
        WishlistItem.Priority priority, // Stores the priority of the item.
        WishlistItem.WishlistCategory category, // Stores the category of the item.
        String notes, // Stores the optional notes of the item.
        WishlistItem.WishlistStatus status, // Stores whether the item is pending or already purchased.
        LocalDate purchaseDate, // Stores the purchase date when the item has already been bought.
        WishlistItem.PurchasePaymentMethod paymentMethod, // Stores the payment method chosen when the purchase happened.
        Integer installments, // Stores how many installments belong to the purchase.
        Boolean firstInstallmentNextMonth, // Stores whether the first installment starts in the next month.
        LocalDateTime createdAt, // Stores when the item was created.
        LocalDateTime updatedAt // Stores when the item was last updated.
) { // Closes the record declaration.
} // Closes the DTO type.
