package com.controledegastos.backend.wishlist.dto; // Declares the package for wishlist write DTOs.

import com.controledegastos.backend.wishlist.WishlistItem; // Imports the enums reused by the request DTO.

import java.math.BigDecimal; // Imports the money type used by price fields.

public record WishlistRequestDTO( // Declares the immutable DTO used to create or update wishlist items.
        String description, // Stores the visible name/description of the item.
        BigDecimal originalPrice, // Stores the original price informed by the user.
        BigDecimal discountPercent, // Stores the optional discount percentage informed by the user.
        WishlistItem.Priority priority, // Stores the desired priority of the item.
        WishlistItem.WishlistCategory category, // Stores the category chosen by the user.
        String notes // Stores the optional notes written by the user.
) { // Closes the record declaration.
} // Closes the DTO type.
