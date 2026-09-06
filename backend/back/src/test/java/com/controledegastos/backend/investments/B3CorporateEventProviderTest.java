package com.controledegastos.backend.investments;

import com.controledegastos.backend.investments.CorporateEvent.EventType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class B3CorporateEventProviderTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @ParameterizedTest
    @CsvSource({
            "PETR4,DIVIDENDO,DIVIDENDO",
            "BBAS3,JRS CAP PROPRIO,JCP",
            "VALE3,DIVIDENDO,DIVIDENDO",
            "MXRF11,RENDIMENTO,DIVIDENDO",
            "HGLG11,RENDIMENTO,DIVIDENDO",
            "KNRI11,RENDIMENTO,DIVIDENDO",
            "KNCA11,RENDIMENTO,DIVIDENDO",
            "XPCA11,RENDIMENTO,DIVIDENDO",
            "RURA11,RENDIMENTO,DIVIDENDO"
    })
    void preservesPaymentDateForStocksFiisAndFiagros(String symbol, String label, EventType expectedType) throws Exception {
        String response = """
                {
                  \"cashDividends\": [{
                    \"label\": \"%s\",
                    \"assetIssued\": \"%s\",
                    \"isinCode\": \"BRTESTE\",
                    \"rate\": \"0,12345678\",
                    \"lastDatePrior\": \"01/09/2026\",
                    \"paymentDate\": \"15/09/2026\"
                  }]
                }
                """.formatted(label, symbol);

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
}
