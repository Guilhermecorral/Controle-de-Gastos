package com.controledegastos.backend.investments;

import com.controledegastos.backend.investments.CorporateEvent.EventType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class B3CorporateEventProviderTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @ParameterizedTest
    @CsvSource({
            "PETR4,DIVIDENDO,DIVIDENDO",
            "BBAS3,JRS CAP PROPRIO,JCP",
            "VALE3,DIVIDENDO,DIVIDENDO",
            "MXRF11,RENDIMENTO,RENDIMENTO",
            "HGLG11,RENDIMENTO,RENDIMENTO",
            "KNRI11,RENDIMENTO,RENDIMENTO",
            "KNCA11,RENDIMENTO,RENDIMENTO",
            "XPCA11,RENDIMENTO,RENDIMENTO",
            "RURA11,RENDIMENTO,RENDIMENTO"
    })
    void preservesPaymentDateForStocksFiisAndFiagros(String symbol, String label, EventType expectedType) throws Exception {
        String response = """
                {
                  \"cashDividends\": [{
                    \"label\": \"%s\",
                    \"assetIssued\": \"%s\",
                    \"isinCode\": \"%s\",
                    \"rate\": \"0,12345678\",
                    \"lastDatePrior\": \"01/09/2026\",
                    \"paymentDate\": \"15/09/2026\"
                  }]
                }
                """.formatted(label, symbol, isinFor(symbol));

        var events = B3CorporateEventProvider.parseResponse(symbol, Set.of(symbol), objectMapper.readTree(response));

        assertThat(events).singleElement().satisfies(event -> {
            assertThat(event.symbol()).isEqualTo(symbol);
            assertThat(event.eventType()).isEqualTo(expectedType);
            assertThat(event.exDate()).hasToString("2026-09-01");
            assertThat(event.paymentDate()).hasToString("2026-09-15");
            assertThat(event.amountPerUnit()).isEqualByComparingTo("0.12345678");
            assertThat(event.taxRate()).isEqualByComparingTo(expectedType == EventType.JCP ? new BigDecimal("15") : BigDecimal.ZERO);
            assertThat(event.source()).isEqualTo("B3_EXPERIMENTAL");
        });
    }

    @org.junit.jupiter.api.Test
    void rejectsPreferredShareEventWhenOnlyOrdinaryTickerIsHeld() throws Exception {
        String response = """
                { "cashDividends": [{
                  "label": "JRS CAP PROPRIO", "assetIssued": "BRBBDCACNPR8", "isinCode": "BRBBDCACNPR8",
                  "rate": "0,01000000", "lastDatePrior": "01/09/2026", "paymentDate": "01/10/2026"
                }] }
                """;

        var events = B3CorporateEventProvider.parseResponse("BBDC", Set.of("BBDC3"), objectMapper.readTree(response));

        assertThat(events).singleElement().satisfies(event -> {
            assertThat(event.symbol()).isNull();
            assertThat(event.resolutionStatus()).isEqualTo("TICKER_AMBIGUO");
        });
    }

    @org.junit.jupiter.api.Test
    void rejectsAnExplicitPreferredTickerWhenTheWalletOnlyHoldsOrdinaryShares() throws Exception {
        String response = """
                { "cashDividends": [{
                  "label": "DIVIDENDO", "assetIssued": "BBDC4", "isinCode": "BRBBDCACNPR8",
                  "rate": "0,01000000", "lastDatePrior": "01/09/2026", "paymentDate": "01/10/2026"
                }] }
                """;

        var events = B3CorporateEventProvider.parseResponse("BBDC", Set.of("BBDC3"), objectMapper.readTree(response));

        assertThat(events).singleElement().satisfies(event -> {
            assertThat(event.symbol()).isNull();
            assertThat(event.resolutionStatus()).isEqualTo("TICKER_AMBIGUO");
        });
    }

    @org.junit.jupiter.api.Test
    void resolvesTheIsinFormatReturnedByB3ForBbas3() throws Exception {
        String response = """
                [{ "cashDividends": [{
                  "label": "JRS CAP PROPRIO", "assetIssued": "BRBBASA04OR8", "isinCode": "BRBBASA04OR8",
                  "rate": "0,10245643978", "lastDatePrior": "01/09/2026", "paymentDate": "11/09/2026"
                }] }]
                """;

        var events = B3CorporateEventProvider.parseResponse("BBAS", Set.of("BBAS3"), objectMapper.readTree(response));

        assertThat(events).singleElement().satisfies(event -> {
            assertThat(event.symbol()).isEqualTo("BBAS3");
            assertThat(event.resolutionStatus()).isEqualTo("VALIDO");
            assertThat(event.paymentDate()).hasToString("2026-09-11");
        });
    }

    @org.junit.jupiter.api.Test
    void ignoresTechnicalFundSeriesInsteadOfTreatingThemAsFundShares() throws Exception {
        String response = """
                { "cashDividends": [
                  { "label": "RENDIMENTO", "assetIssued": "MXRF11", "isinCode": "BRMXRFCTF008", "rate": "0,10000000", "lastDatePrior": "31/07/2026", "paymentDate": "14/08/2026" },
                  { "label": "RENDIMENTO", "assetIssued": "MXRF11", "isinCode": "BRMXRFR24M16", "rate": "0,02000000", "lastDatePrior": "31/07/2026", "paymentDate": "14/08/2026" },
                  { "label": "RENDIMENTO", "assetIssued": "MXRF11", "isinCode": "BRMXRFR25M15", "rate": "0,06000000", "lastDatePrior": "31/07/2026", "paymentDate": "14/08/2026" }
                ] }
                """;

        var events = B3CorporateEventProvider.parseResponse("MXRF", Set.of("MXRF11"), objectMapper.readTree(response));

        assertThat(events).singleElement().satisfies(event -> {
            assertThat(event.symbol()).isEqualTo("MXRF11");
            assertThat(event.amountPerUnit()).isEqualByComparingTo("0.10");
        });
    }

    @org.junit.jupiter.api.Test
    void keepsResolvedEventsWhenAnAmbiguousClassHasNoTicker() {
        LocalDate exDate = LocalDate.of(2026, 9, 1);
        var ambiguous = new MarketDataProvider.CorporateEventData("b3:ambiguous", null, "BRBBDCACNPR8", EventType.JCP,
                null, new BigDecimal("0.01"), new BigDecimal("15"), exDate, exDate.plusDays(10), "B3", "TICKER_AMBIGUO");
        var resolved = new MarketDataProvider.CorporateEventData("b3:bbas3", "BBAS3", "BRBBASA04OR8", EventType.JCP,
                null, new BigDecimal("0.10"), new BigDecimal("15"), exDate, exDate.plusDays(10), "B3", "VALIDO");

        var events = B3CorporateEventProvider.sortEligibleEvents(exDate, List.of(ambiguous, resolved));

        assertThat(events).extracting(MarketDataProvider.CorporateEventData::symbol)
                .containsExactly("BBAS3", null);
    }

    private String isinFor(String symbol) {
        String root = symbol.replaceAll("\\d+$", "");
        if (symbol.endsWith("11")) return "BR" + root + "CTF001";
        return "BRTESTE" + (symbol.endsWith("3") ? "OR1" : "PR1");
    }
}
