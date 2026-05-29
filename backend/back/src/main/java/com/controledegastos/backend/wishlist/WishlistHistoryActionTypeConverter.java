package com.controledegastos.backend.wishlist;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Lê ações antigas do histórico sem quebrar a trilha de eventos da wishlist.
 */
@Converter(autoApply = false)
public class WishlistHistoryActionTypeConverter implements AttributeConverter<WishlistHistoryEntry.ActionType, String> {

    @Override
    public String convertToDatabaseColumn(WishlistHistoryEntry.ActionType attribute) {
        return attribute == null ? null : attribute.name();
    }

    @Override
    public WishlistHistoryEntry.ActionType convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return null;
        }

        return switch (dbData) {
            case "CRIADO", "CREATED" -> WishlistHistoryEntry.ActionType.CREATED;
            case "ATUALIZADO", "UPDATED" -> WishlistHistoryEntry.ActionType.UPDATED;
            case "COMPRADO", "PURCHASED" -> WishlistHistoryEntry.ActionType.PURCHASED;
            case "COMPRA_DESFEITA", "PURCHASE_UNDONE" -> WishlistHistoryEntry.ActionType.PURCHASE_UNDONE;
            case "MOVIDO", "MOVED" -> WishlistHistoryEntry.ActionType.MOVED;
            case "EXCLUIDO", "EXCLUÍDO", "DELETED" -> WishlistHistoryEntry.ActionType.DELETED;
            default -> WishlistHistoryEntry.ActionType.valueOf(dbData);
        };
    }
}
