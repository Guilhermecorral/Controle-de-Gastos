package com.controledegastos.backend.wishlist;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Mantém compatibilidade com valores antigos de prioridade já gravados no banco.
 */
@Converter(autoApply = false)
public class WishlistPriorityConverter implements AttributeConverter<WishlistItem.Priority, String> {

    @Override
    public String convertToDatabaseColumn(WishlistItem.Priority attribute) {
        return attribute == null ? null : attribute.name();
    }

    @Override
    public WishlistItem.Priority convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return null;
        }

        return switch (dbData) {
            case "ALTA", "ALTO" -> WishlistItem.Priority.ALTO;
            case "BAIXA", "BAIXO" -> WishlistItem.Priority.BAIXO;
            default -> WishlistItem.Priority.valueOf(dbData);
        };
    }
}
