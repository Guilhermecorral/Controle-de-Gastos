package com.controledegastos.backend.wishlist.dto;

import java.math.BigDecimal;

/**
 * Resume os totais principais exibidos na visao de resumo da wishlist.
 */
public record WishlistSummaryDTO(
        long quantidadeItensDesejados,
        long quantidadeItensComprados,
        BigDecimal valorTotalDesejados,
        BigDecimal valorTotalComprados
) {
}
