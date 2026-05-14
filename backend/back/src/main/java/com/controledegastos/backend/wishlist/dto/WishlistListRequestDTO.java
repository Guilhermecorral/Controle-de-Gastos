package com.controledegastos.backend.wishlist.dto;

/**
 * Representa os dados usados para criar ou editar uma lista nomeada da wishlist.
 */
public record WishlistListRequestDTO(
        String name,
        String description
) {
}
