package com.controledegastos.backend.investments;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface MarketDataProvider {
    List<CorporateEventData> corporateEvents(LocalDate referenceDate);

    record CorporateEventData(String sourceReference, String symbol, CorporateEvent.EventType eventType,
                              String payerCnpj, BigDecimal amountPerUnit, BigDecimal taxRate,
                              LocalDate exDate, LocalDate paymentDate, String source) {}
}
