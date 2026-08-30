package com.controledegastos.backend.wishlist.dto;

import java.util.List;

public record WishlistImportPreviewResponseDTO(
        String format,
        List<WishlistImportPreviewItemDTO> items,
        List<String> warnings
) {
}
