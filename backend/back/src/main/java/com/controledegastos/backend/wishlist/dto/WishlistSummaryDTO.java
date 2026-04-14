package com.controledegastos.backend.wishlist.dto; // Declares the package for wishlist summary DTOs.

import java.math.BigDecimal; // Imports the money type used by summary values.

public record WishlistSummaryDTO( // Declares the immutable DTO returned by the summary endpoint.
        long quantidadeItensDesejados, // Stores how many pending items exist.
        long quantidadeItensComprados, // Stores how many purchased items exist.
        BigDecimal valorTotalDesejados, // Stores the total final price of pending items.
        BigDecimal valorTotalComprados // Stores the total final price of purchased items.
) { // Closes the record declaration.
} // Closes the DTO type.
