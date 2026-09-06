package com.controledegastos.backend.investments;

import org.springframework.stereotype.Component;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@Component
@ConditionalOnProperty(name = "app.investments.corporate-events.provider", havingValue = "mock", matchIfMissing = true)
public class MockMarketDataProvider implements MarketDataProvider {
    @Override
    public List<CorporateEventData> corporateEvents(LocalDate referenceDate, Set<String> symbols) {
        LocalDate exDate = referenceDate.minusDays(2);
        return List.of(
                new CorporateEventData("mock:PETR4:DIVIDENDO:" + exDate, "PETR4", CorporateEvent.EventType.DIVIDENDO,
                        "33.000.167/0001-01", new BigDecimal("0.65000000"), BigDecimal.ZERO,
                        exDate, referenceDate.plusDays(7), "MOCK"),
                new CorporateEventData("mock:BBAS3:JCP:" + exDate, "BBAS3", CorporateEvent.EventType.JCP,
                        "00.000.000/0001-91", new BigDecimal("0.20000000"), new BigDecimal("15.0000"),
                        exDate, referenceDate, "MOCK")
        );
    }
}
