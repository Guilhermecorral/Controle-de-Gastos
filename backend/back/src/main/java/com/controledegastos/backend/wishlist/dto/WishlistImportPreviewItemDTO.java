package com.controledegastos.backend.wishlist.dto;

import com.controledegastos.backend.wishlist.WishlistItem;

import java.math.BigDecimal;

public record WishlistImportPreviewItemDTO(
        int index,
        String description,
        BigDecimal originalPrice,
        WishlistItem.Priority priority,
        WishlistItem.WishlistCategory category,
        String notes,
        String suggestedListName,
        boolean selectedByDefault,
        String rationale
) {
}
