package com.controledegastos.backend.investments;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class B3HistoricalCashDividendProviderTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void preservesHistoricalEventsWithoutInventingAPaymentDate() throws Exception {
        String payload = """
                { "results": [
                  { "corporateAction": "DIVIDENDO", "isinCode": "BRBBASA04OR8", "valueCash": "0,12345678",
                    "lastDatePriorEx": "01/09/2026", "paymentDate": "" },
                  { "corporateAction": "DIVIDENDO", "isinCode": "BRPETRACNPR6", "valueCash": "0,65432100",
                    "lastDatePriorEx": "01/09/2026" }
                ] }
                """;

        var records = B3HistoricalCashDividendProvider.parsePage("BBAS3", "BRBBASA04OR8", objectMapper.readTree(payload));

        assertThat(records).singleElement().satisfies(record -> {
            assertThat(record.symbol()).isEqualTo("BBAS3");
            assertThat(record.exDate()).hasToString("2026-09-01");
            assertThat(record.paymentDate()).isNull();
            assertThat(record.amountPerUnit()).isEqualByComparingTo("0.12345678");
        });
    }
}
