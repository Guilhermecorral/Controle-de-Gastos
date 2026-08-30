package com.controledegastos.backend.wishlist.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record WishlistImportRequestDTO(
        @NotEmpty(message = "Selecione ao menos um desejo para importar")
        @Size(max = 500, message = "Importe no maximo 500 desejos por vez")
        List<@Valid WishlistImportItemDTO> items
) {
}
