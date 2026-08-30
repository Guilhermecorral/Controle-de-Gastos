package com.controledegastos.backend.wishlist.dto;

import com.controledegastos.backend.wishlist.WishlistItem;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record WishlistImportItemDTO(
        @NotBlank(message = "Todo desejo importado precisa de um nome")
        @Size(max = 255, message = "O nome do desejo deve ter no maximo 255 caracteres")
        String description,
        @DecimalMin(value = "0.00", message = "O preco de um desejo nao pode ser negativo")
        BigDecimal originalPrice,
        WishlistItem.Priority priority,
        WishlistItem.WishlistCategory category,
        @Size(max = 500, message = "As observacoes devem ter no maximo 500 caracteres")
        String notes,
        Long listId
) {
}
