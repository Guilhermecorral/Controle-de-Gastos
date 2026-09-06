package com.controledegastos.backend.investments;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

public interface MarketDataProvider {
    /**
     * Returns only events for symbols currently needed by a wallet sync. Providers must not
     * scan the entire market as part of a user-triggered action.
     */
    List<CorporateEventData> corporateEvents(LocalDate referenceDate, Set<String> symbols);

    record CorporateEventData(String sourceReference, String symbol, String isinCode, CorporateEvent.EventType eventType,
                              String payerCnpj, BigDecimal amountPerUnit, BigDecimal taxRate,
                              LocalDate exDate, LocalDate paymentDate, String source, String resolutionStatus) {}
}
