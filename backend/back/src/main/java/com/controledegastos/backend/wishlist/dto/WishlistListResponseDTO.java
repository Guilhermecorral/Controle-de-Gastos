package com.controledegastos.backend.wishlist.dto;

import java.time.LocalDateTime;

/**
 * Representa os dados devolvidos ao listar ou consultar listas da wishlist.
 */
public record WishlistListResponseDTO(
        Long id,
        String name,
        String description,
        Boolean isDefault,
        long itemCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
